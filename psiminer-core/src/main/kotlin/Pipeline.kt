import com.intellij.openapi.project.Project
import filter.Filter
import labelextractor.LabelExtractor
import me.tongfei.progressbar.ProgressBar
import org.slf4j.LoggerFactory
import psi.Parser
import psi.ParserException
import psi.language.JavaHandler
import psi.language.KotlinHandler
import psi.printTree
import psi.transformations.PsiTreeTransformation
import storage.Storage
import java.io.File
import kotlin.concurrent.thread

class Pipeline(
    val language: Language,
    private val repositoryOpener: PipelineRepositoryOpener,
    psiTreeTransformations: List<PsiTreeTransformation>,
    private val filters: List<Filter>,
    val labelExtractor: LabelExtractor,
    val storage: Storage
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val languageHandler = when (language) {
        Language.Java -> JavaHandler()
        Language.Kotlin -> KotlinHandler()
    }

    private val parser = Parser(languageHandler, psiTreeTransformations, labelExtractor.granularityLevel)

    private fun checkFolderIsDataset(folder: File): Boolean {
        val folderDirNames = folder.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: return false
        return Dataset.values().all { folderDirNames.contains(it.folderName) }
    }

    fun extract(
        inputDirectory: File,
        batchSize: Int = 1,
        printTrees: Boolean = false
    ) {
        require(batchSize > 0) { "Amount threads must be positive." }
        println("Parser configuration:\n$parser")
        val isDataset = checkFolderIsDataset(inputDirectory)
        if (isDataset) {
            println("Dataset structure is detected.")
            Dataset.values().forEach { holdout ->
                val holdoutFolder = inputDirectory.resolve(holdout.folderName)
                val holdoutRepositories = holdoutFolder
                    .walk().maxDepth(1).toList().filter { it.name != holdout.folderName && !it.isFile }
                holdoutRepositories.forEachIndexed { index, holdoutRepositoryRoot ->
                    println(
                        "Process ${holdoutRepositoryRoot.name} from $holdout (${index + 1}/${holdoutRepositories.size})"
                    )
                    processRepository(holdoutRepositoryRoot, holdout, batchSize, printTrees)
                }
            }
        } else {
            println("No dataset found. Process all sources under passed path")
            processRepository(inputDirectory, null, batchSize, printTrees)
        }
    }

    private fun processRepository(
        repositoryRoot: File,
        holdout: Dataset?,
        batchSize: Int = 1,
        printTrees: Boolean = false
    ) {
        repositoryOpener.openRepository(repositoryRoot) { project ->
            processProject(project, holdout, batchSize, printTrees)
        }
    }

    private fun processProject(
        project: Project,
        holdout: Dataset?,
        batchSize: Int = 1,
        printTrees: Boolean = false
    ) {
        logger.warn("Process project ${project.name}")
        val projectFiles = extractProjectFiles(project, language)

        val progressBar = ProgressBar(project.name, projectFiles.size.toLong())

        projectFiles.chunked(batchSize).forEach { files ->
            val threads = files.map { file ->
                thread {
                    try {
                        parser.parseFile(file, project) { psiRoot ->
                            if (filters.any { !it.validateTree(psiRoot, languageHandler) }) return@parseFile
                            val labeledTree =
                                labelExtractor.extractLabel(psiRoot, languageHandler) ?: return@parseFile
                            synchronized(storage) {
                                storage.store(labeledTree, holdout)
                                if (printTrees) labeledTree.root.printTree()
                            }
                        }
                    } catch (exception: ParserException) {
                        logger.error("Error while parsing ${exception.filepath}")
                    } finally {
                        progressBar.step()
                    }
                }
            }
            threads.forEach { it.join() }
        }

        progressBar.close()
    }
}

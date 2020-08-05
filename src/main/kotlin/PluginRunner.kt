import astminer.paths.PathMiner
import astminer.paths.PathRetrievalSettings
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import psi.extractPsiFromProject
import storage.XCode2SeqPathStorage
import storage.XCode2VecPathStorage
import storage.XPathContextsStorage
import kotlin.system.exitProcess

class PluginRunner : ApplicationStarter {

    override fun getCommandName(): String = "psiminer"

    override fun main(args: Array<out String>) = PsiExtractor().main(args.slice(1 until args.size))
}

class PsiExtractor : CliktCommand() {

    private val dataset: String by option("--dataset", help = "Path to dataset").required()
    private val output: String by option("--output", help = "Output directory").required()

    private fun buildStorage(): XPathContextsStorage<String> {
        return when (Config.storage) {
            "code2seq" -> XCode2SeqPathStorage(output)
            "code2vec" -> XCode2VecPathStorage(output)
            else -> throw UnsupportedOperationException("Unknown storage ${Config.storage}")
        }
    }

    override fun run() {
        println("Opening data as a project...")
        val project = ProjectUtil.openOrImport(dataset, null, true) ?: let {
            println("Could not load project from $dataset")
            exitProcess(0)
        }

        val storage = buildStorage()
        val miner = PathMiner(PathRetrievalSettings(
            Config.maxPathHeight,
            Config.maxPathWidth
        ))

        println("Start extracting psi from ${project.name} project...")
        val datasetStatistic = extractPsiFromProject(project, storage, miner)
        println("Extracted data statistic:\n$datasetStatistic")

        storage.close()
        exitProcess(0)
    }
}
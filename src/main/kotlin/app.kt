import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.ImageIOUtil
import org.bytedeco.javacpp.tesseract
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import org.bytedeco.javacpp.lept.*
import org.bytedeco.javacpp.tesseract.*
import org.bytedeco.javacpp.*
import org.apache.commons.io.FileUtils
import org.apache.pdfbox.pdmodel.PDPage

/**
 * Created by gcan on 04.03.2016.
 */

fun main(args:Array<String>){
    val classLoader = ClassLoader.getSystemClassLoader();

    val pdfFile = File(classLoader.getResource("aa.pdf")!!.getFile())

    val api: TessBaseAPI
    val trainedData = File(Files.createTempDirectory("emsal").toString(), "tessdata")
    val trainedDataResource = classLoader.getResource("tur.traineddata")
    val trainedDataFile = File(trainedData.toString(), "tur.traineddata")
    trainedDataResource.openStream().copyTo( FileUtils.openOutputStream(trainedDataFile))

    api = TessBaseAPICreate()
    val result = TessBaseAPIInit3(api, trainedData.getAbsolutePath().toString(), "tur")
    // Initialize tesseract-ocr with "tur", without specifying tessdata path
    if (result != 0) {
        TessBaseAPIDelete(api)
        System.err.println("Could not initialize tesseract.")
        return
    }

    val document = PDDocument.loadNonSeq(pdfFile, null)
    val pdPages:List<PDPage> = document.getDocumentCatalog().getAllPages() as List<PDPage>
    var page = 0


    for (pdPage in pdPages) {
        ++page
        val bim = pdPage.convertToImage(BufferedImage.TYPE_INT_RGB, 300)
        val temporaryImage = File(trainedData.toString(), pdfFile.getName() + "-" + page + ".png")
        ImageIOUtil.writeImage(bim, temporaryImage.toString(), 300)


        val image = pixRead(temporaryImage.toString())
        api.SetImage(image)
        // Get OCR result
        val outText = api.GetUTF8Text()
        println("OCR output:\n" + outText.string)
        outText.deallocate()
        // Destroy used object and release memory
        pixDestroy(image)
    }
    document.close()
    api.End()

}
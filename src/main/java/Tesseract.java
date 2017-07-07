import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.tesseract;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixRead;

/**
 * Created by gcan on 24.03.2016.
 */
public class Tesseract {

    private tesseract.TessBaseAPI api;
    private final File trainedDataFolder;

    public Tesseract() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        trainedDataFolder = new File(Files.createTempDirectory("emsal").toString(), "tessdata");

        URL trainedDataResource = classLoader.getResource("tur.traineddata");
        File trainedDataFile = new File(trainedDataFolder.toString(), "tur.traineddata");
        IOUtils.copy(trainedDataResource.openStream(), FileUtils.openOutputStream(trainedDataFile));

        api = tesseract.TessBaseAPICreate();
        int result = tesseract.TessBaseAPIInit3(api, trainedDataFolder.getAbsolutePath().toString(), "tur");
        // Initialize tesseract-ocr with "tur", without specifying tessdata path
        if (result != 0) {
            tesseract.TessBaseAPIDelete(api);
            System.err.println("Could not initialize tesseract.");
            return;
        }
    }

    public void dispose() {
        api.End();
    }

    public static String withTesseract(Function<Tesseract, String> consumer) throws IOException {

        Tesseract tesseract = new Tesseract();
        try {
            return consumer.apply(tesseract);
        } finally {
            tesseract.dispose();
        }
    }

    public String operate(File pdfFile) {
        PDDocument document = null;
        StringBuilder stringBuilder = new StringBuilder("");
        try {
            document = PDDocument.loadNonSeq(pdfFile, null);
            List<PDPage> pdPages = document.getDocumentCatalog().getAllPages();
            int page = 0;


            for (PDPage pdPage : pdPages) {
                System.out.println(page);
                ++page;
                BufferedImage bim = pdPage.convertToImage(BufferedImage.TYPE_INT_RGB, 300);
                File temporaryImage = new File(trainedDataFolder.toString(), "pdf" + page + ".png");
                ImageIOUtil.writeImage(bim, temporaryImage.toString(), 300);


                lept.PIX image = pixRead(temporaryImage.toString());
                api.SetImage(image);
                // Get OCR result
                BytePointer outText = api.GetUTF8Text();
                if (outText != null) {
                    stringBuilder.append(outText.getString());
                    outText.deallocate();
                }
                // Destroy used object and release memory
                pixDestroy(image);
            }
            document.close();
        } catch (IOException e) {
            System.out.print(e.getCause());
        }

        return stringBuilder.toString();
    }
}

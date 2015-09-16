


import org.apache.commons.lang3.StringUtils;


import java.io.*;

public class FileReader {
  
    private static Logger logger = Logger.getLogger(FileReader.class.getName());

    public static String loadFile(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        String line = "";
        String ls = System.getProperty("line.separator");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName)), getCharsetDecoder()))) {
            while ((line = reader.readLine()) != null) {
               
                stringBuilder.append(ls);
            }
        } catch (IOException e) {
            e.printStackTrace();
            MiscUtil.exitWithError();
        }
        logger.info("file " + fileName + " loaded into a memory");
        return stringBuilder.toString();
    }

	   public static CharsetDecoder getCharsetDecoder() {
        CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        return decoder;
    }

}

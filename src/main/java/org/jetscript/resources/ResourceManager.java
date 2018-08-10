package org.jetscript.resources;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class ResourceManager {

    private static Log logger = LogFactoryUtil.getLog(ResourceManager.class);

    public static boolean findResource(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       File baseDir) {
        String pathInfo = request.getPathInfo();
        File f = new File(baseDir,pathInfo);
        if (!f.exists()) return false;
        return processFile(f,response);
    }

    private static boolean processFile(File target, HttpServletResponse response) {
        try {
            response.setContentType(identifyFileType(target));
            response.setStatus(200);
            byte[] fileContent = Files.readAllBytes(target.toPath());
            OutputStream os = response.getOutputStream();
            response.setContentLength(fileContent.length);
            os.write(fileContent);
            os.flush();
            return true;
        } catch (IOException ex) {
            logger.error(ex);
            return false;
        }
    }

    private static String identifyFileType(File file) {

        String fileType = "Undetermined";
    
        try {
            fileType = Files.probeContentType(file.toPath());
        } catch (IOException ioException) {
            logger.error("ERROR: Unable to determine file type for " + file.getAbsolutePath()
                               + " due to exception " + ioException);
        }
        return fileType;
    }

}
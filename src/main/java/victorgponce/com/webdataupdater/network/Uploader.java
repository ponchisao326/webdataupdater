package victorgponce.com.webdataupdater.network;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.File;

public class Uploader {

    public static void postMethod(String filePath, String serverUrl) {
        String boundary = "Boundary-" + System.currentTimeMillis();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("El archivo no existe: " + filePath);
            return;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = connection.getOutputStream()) {

                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
                os.write(("Content-Type: application/json\r\n\r\n").getBytes());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }

                os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            }

            int responseCode = connection.getResponseCode();

            InputStream inputStream = (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST)
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

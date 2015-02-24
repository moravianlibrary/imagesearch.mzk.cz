package net.semanticmetadata.importer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.lang.String;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("You must specify two parameters: imageDir, url");
            System.exit(1);
        }
        File imageDir = new File(args[0]);
        String url = args[1];

        if (!imageDir.isDirectory()) {
            System.err.println("First parameter must be path to a directory with images.");
            System.exit(1);
        }
        List<String> images = new ArrayList<String>();
        for (File file : imageDir.listFiles()) {
            images.add(file.getName());
        }
        importImages(images, url);
    }

    private static void importImages(List<String> images, String url) {
        // Prepare request
        JSONArray request = new JSONArray();
        int i = 0;
        int count = images.size();
        for (String image : images) {
            JSONObject item = new JSONObject();
            item.put("id", image);
            item.put("url", url + image);
            item.put("rights", "CC-BY");
            item.put("provider", "Name of institution");
            item.put("provider_link", "http://localhost");
            item.put("index", false);
            request.add(item);
            i++;

            if (request.size() == 5) {
                try {
                    URLConnection connection = new URL("http://localhost:8983/solr/liresolr/indexImage").openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Accept-Charset", "UTF-8");
                    connection.setRequestProperty("Content-Type", "application/json");

                    PrintWriter output = new PrintWriter(connection.getOutputStream());
                    output.print(request.toJSONString());
                    output.flush();
                    output.close();

                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    int c;
                    while ((c = reader.read()) != -1) {
                        System.out.print((char) c);
                    }
                    request.clear();
                    System.out.println(String.format("Processed %d/%d images", i, count));

                } catch (IOException e) {
                    System.out.println(e.getLocalizedMessage());
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
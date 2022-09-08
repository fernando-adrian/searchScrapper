package com.cursojava.curso.services;

import com.cursojava.curso.entities.WebPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hibernate.internal.util.StringHelper.isBlank;

@Service
public class SpiderService {

    @Autowired
    private SearchService searchService;

    public void indexWebPages() {
        List<WebPage> linksToIndex = searchService.getLinksToIndex();
        linksToIndex.stream().parallel().forEach(webPage -> {
            try {
                indexWebPage(webPage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void indexWebPage(WebPage webPage) {
        String url = webPage.getUrl();
        String content = getWebContent(url);
        String domain = getDomain(url);

        if (isBlank(content)) return;
        indexAndSaveWebPage(webPage, content);
        saveLinks(domain, content);
    }

    private String getDomain(String url) {
        String[] aux = url.split("/");
        return aux[0] + "//" + aux[2];
    }

    private void saveLinks(String domain, String content) {
        List<String> links = getLinks(domain, content);
        links.stream()
                .filter(link -> !searchService.exist(link))
                .map(link -> new WebPage(link))
                .forEach(webPage -> searchService.save(webPage));
    }

    public List<String> getLinks(String domain, String content) {
        List<String> links = new ArrayList<>();

        String[] splitHref = content.split("href=\"");
        List<String> listHref = Arrays.asList(splitHref);
//        listHref.remove(0);
        listHref.forEach(left -> {
            String[] aux = left.split("\"");
            links.add(aux[0]);
        });

        return cleanLinks(domain, links);
    }

    private List<String> cleanLinks(String domain, List<String> links) {
        String[] excludedExtensions = new String[]{".css", ".png", ".jpeg", ".jpg", ".gif", "js"};

        List<String> result = links.stream()
                .filter(link -> Arrays.stream(excludedExtensions).noneMatch(link::endsWith))
                .map(link -> link.startsWith("/") ? domain + link : link)
                .collect(Collectors.toList());
        return result;
    }

    private void indexAndSaveWebPage(WebPage webPage, String content) {
        String title = getTitle(content);
        String description = getDescription(content);

        webPage.setTitle(title);
        webPage.setDescription(description);
        searchService.save(webPage);
    }

    public String getTitle(String content) {
        String[] splitAux = content.split("<title>");
        String[] splitAux2 = splitAux[1].split("</title>");
        return splitAux2[0];
    }

    public String getDescription(String content) {
        String[] splitAux = content.split("<meta name=\"description\" content=\"");
        String[] splitAux2 = splitAux[1].split("\">");
        return splitAux2[0];
    }

    private String getWebContent(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String encoding = conn.getContentEncoding();

            InputStream input = conn.getInputStream();

            Stream<String> lines = new BufferedReader(new InputStreamReader(input))
                    .lines();
            System.out.println("END");

            return lines.collect(Collectors.joining());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}

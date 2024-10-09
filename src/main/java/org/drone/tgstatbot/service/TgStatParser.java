package org.drone.tgstatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.drone.tgstatbot.dao.TgStatData;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class TgStatParser {


    public TgStatData getResponse(String channelName) throws HttpStatusException {
        String response = getTgStarterResponse(channelName);
        if (response == null) {
            return null;
        }

        TgStatData tgStatData = getData(response);

        if (tgStatData.getName() == null || tgStatData.getSubscribers() == null || tgStatData.getCoverage() == null) {
            return null;
        }
        return tgStatData;
    }


    private String getTgStarterResponse(String channelName) throws HttpStatusException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://tgstat.ru/channel/@" + channelName + "/stat"))
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpResponse<String> response;

        try (var client = HttpClient.newBuilder().build()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Error while trying to get page {}", channelName);
            log.error(e.getMessage(), e);
            return null;
        }
        if(response.statusCode()!= 200){
            throw new HttpStatusException("Error statusCode", response.statusCode(), channelName);
        }

        return response.body();
    }

    private TgStatData getData(String response) {
        TgStatData tgStatData = new TgStatData();
        var doc = Jsoup.parse(response);
        var elem = doc.selectXpath("//div[@class='col-12 col-sm-7 col-md-8 col-lg-6']/h1").first();
        if (elem != null) {
            tgStatData.setName(elem.text());
        }
        var elements = doc.selectXpath("//div[@class='row justify-content-center mb-n3']/div");
        for (var element : elements) {
            if (element.html().contains("подписчики")) {
                var h2 = Jsoup.parse(element.html()).selectXpath("//h2");
                tgStatData.setSubscribers(h2.getFirst().text());
            }
            if (element.html().contains("средний охват") && element.html().contains("1 публикации")) {
                var h2 = Jsoup.parse(element.html()).selectXpath("//h2");
                tgStatData.setCoverage(h2.getFirst().text());
            }
        }
        return tgStatData;
    }
}

package com.google.ar.sceneform.samples.hellosceneform;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * implements Async posting for the okhttp library.
 */

public class AsyncHttp {

    public static String responseData = "";

    private static void setResponseData(String data) {
        responseData = data;
    }


    private final static OkHttpClient client = new OkHttpClient();
    //This is the IP of my local machine running the api cause the emulator cannot resolve local host.
    private final static String processorURL = "https://image-processor.cfapps.io/image";
    private static final MediaType MEDIA_TYPE = MediaType.parse("image/png");

    public static CompletableFuture<String> postImage(File file) throws IOException {
        CompletableFuture<String> f = new CompletableFuture<String>();
        final RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MEDIA_TYPE, file))
                .addFormDataPart("some-field", "some-value")
                .build();


        final Request request = new Request.Builder()
                .url(processorURL)
                .post(requestBody)
                .build();


        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    f.complete("Failed");
                    throw new IOException("Unexpected code " + response);
                } else {
                    String responseBody = response.body().string();
                    setResponseData(responseBody);
                    f.complete(responseBody);
                }

            }
        });
        return f;
    }
}

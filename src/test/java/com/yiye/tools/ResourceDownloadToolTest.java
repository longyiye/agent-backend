package com.yiye.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://aisearch.bj.bcebos.com/homepage/input_panel/aisearch_sample.png";
        String fileName = "baiduAI.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }

}

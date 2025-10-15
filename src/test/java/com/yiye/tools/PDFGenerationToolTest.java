package com.yiye.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "野狼秘籍.pdf";
        String content = "https://github.com/longyiye/agent-backend 野狼交流社区";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }

}

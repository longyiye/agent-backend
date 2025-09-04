package com.yiye.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TerminalOperationToolTest {

    @Test
    public void testExecuteTerminalCommand() {
        TerminalOperationTool tool = new TerminalOperationTool();
        String command = "dir";
        // todo: 当前输出的中文结果会乱码，需解决
        String result = tool.executeTerminalCommand(command);
        assertNotNull(result);
    }
}
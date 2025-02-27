package com.pennet.defender.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private static final int DEFAULT_TIMEOUT = 30; // seconds

    public CommandResult execute(String command) throws IOException {
        return execute(command, DEFAULT_TIMEOUT);
    }

    public CommandResult execute(String command, int timeout) throws IOException {
        logger.info("执行命令: {}", command);

        Process process = Runtime.getRuntime().exec(command);
        List<String> output = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int exitCode;

        try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            // 读取标准输出
            String line;
            while ((line = outputReader.readLine()) != null) {
                output.add(line);
            }

            // 读取错误输出
            while ((line = errorReader.readLine()) != null) {
                errors.add(line);
            }

            // 等待命令执行完成，设置超时
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("命令执行超时: " + command);
            }

            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("命令执行被中断: " + command, e);
        }

        return new CommandResult(exitCode, output, errors);
    }

    public static class CommandResult {
        private final int exitCode;
        private final List<String> output;
        private final List<String> errors;

        public CommandResult(int exitCode, List<String> output, List<String> errors) {
            this.exitCode = exitCode;
            this.output = output;
            this.errors = errors;
        }

        public int getExitCode() {
            return exitCode;
        }

        public List<String> getOutput() {
            return output;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        public String getOutputAsString() {
            return String.join("\n", output);
        }

        public String getErrorsAsString() {
            return String.join("\n", errors);
        }
    }
}
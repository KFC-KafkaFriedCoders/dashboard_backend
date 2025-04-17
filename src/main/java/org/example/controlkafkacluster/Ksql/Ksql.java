package org.example.controlkafkacluster.Ksql;

import io.github.cdimascio.dotenv.Dotenv;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/ksql")
public class Ksql {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String SSH_USER = dotenv.get("SSH_USER");
    private static final String SSH_PRIVATE_KEY = dotenv.get("SSH_PRIVATE_KEY");
    private static final String KSQL_1_HOST = dotenv.get("KSQL_1_HOST");
    private static final String KSQL_2_HOST = dotenv.get("KSQL_2_HOST");
    private static final String KAFKA_START_COMMAND = dotenv.get("KSQL_START_COMMAND");
    private static final String KAFKA_STOP_COMMAND = dotenv.get("KSQL_STOP_COMMAND");
    private static final String KAFKA_STATUS_COMMAND = dotenv.get("KSQL_STATUS_COMMAND");

    @PostMapping("/start/{id}")
    public String start(@PathVariable int id) {
        return executeCommand(getHost(id), KAFKA_START_COMMAND);
    }

    @PostMapping("/shutdown/{id}")
    public String shutdown(@PathVariable int id) {
        return executeCommand(getHost(id), KAFKA_STOP_COMMAND);
    }

    @PostMapping("/status/{id}")
    public String status(@PathVariable int id) {
        String host = getHost(id);
        if (host == null) return "KSQL는 1 ~ 2번까지 있습니다.";

        String result = executeCommand(host, KAFKA_STATUS_COMMAND);

        if (result.trim().isEmpty()) {
            return "Kafka KSQL " + host + " 는 현재 실행 중이지 않습니다.";
        } else {
            return "Kafka KSQL " + host + " 는 정상적으로 실행 중입니다.\n" + result;
        }
    }

    private String getHost(int id) {
        return switch (id) {
            case 1 -> KSQL_1_HOST;
            case 2 -> KSQL_2_HOST;
            default -> null;
        };
    }

    private String executeCommand(String host, String command) {
        if (host == null) return "KSQL은 1 ~ 2번까지 있습니다.";

        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(host);

            KeyProvider keyProvider = ssh.loadKeys(SSH_PRIVATE_KEY);
            ssh.authPublickey(SSH_USER, keyProvider);

            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(command);

                String output = new String(cmd.getInputStream().readAllBytes());
                String error = new String(cmd.getErrorStream().readAllBytes());

                Integer exitStatus = cmd.getExitStatus();

                if (exitStatus == null || exitStatus == 0) {
                    return output;
                } else {
                    return "명령어 입력 실패: " + host + "\n" + error;
                }
            }
        } catch (IOException e) {
            return "명령어 실행 중 오류 발생: " + e.getMessage();
        }
    }
}

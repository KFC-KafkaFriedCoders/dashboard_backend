package org.example.controlkafkacluster.Connect;

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

@RequestMapping("/connect")
@RestController
public class Connect {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String SSH_USER = dotenv.get("SSH_USER");
    private static final String SSH_PRIVATE_KEY = dotenv.get("SSH_PRIVATE_KEY");
    private static final String CONNECT_1_HOST = dotenv.get("CONNECT_1_HOST");
    private static final String CONNECT_2_HOST = dotenv.get("CONNECT_2_HOST");
    private static final String KAFKA_START_COMMAND = dotenv.get("CONNECT_START_COMMAND");
    private static final String KAFKA_STOP_COMMAND = dotenv.get("CONNECT_STOP_COMMAND");
    private static final String KAFKA_STATUS_COMMAND = dotenv.get("CONNECT_STATUS_COMMAND");
    private static final String CONNECT_SOURCE_STATUS_COMMAND = dotenv.get("CONNECT_SOURCE_STATUS_COMMAND");
    private static final String CONNECT_SINK_STATUS_COMMAND = dotenv.get("CONNECT_SINK_STATUS_COMMAND");

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
        if (host == null) return "커넥트는 1 ~ 2번까지 있습니다.";

        String result = executeCommand(host, KAFKA_STATUS_COMMAND);

        // '8083' 포트와 관련된 정보만 필터링
        String filteredResult = filterResult(result, "8083");

        if (filteredResult.trim().isEmpty()) {
            return "Kafka Connect " + host + " 는 현재 실행 중이지 않습니다.";
        } else {
            return "Kafka Connect " + host + " 는 정상적으로 실행 중입니다.\n" + filteredResult;
        }
    }

    private String filterResult(String result, String port) {
        // 결과에서 지정된 포트에 대한 정보를 찾아서 반환
        StringBuilder filtered = new StringBuilder();

        // 각 라인마다 확인하여 지정된 포트 정보만 추출
        for (String line : result.split("\n")) {
            if (line.contains(port)) {
                filtered.append(line.trim()).append("\n");
            }
        }

        return filtered.toString();
    }



    @PostMapping("/status/{id}/{connectorType}")
    public String connectorStatus(@PathVariable int id, @PathVariable String connectorType) {
        String host = getHost(id);
        if (host == null) return "커넥트는 1 ~ 2번까지 있습니다.";

        String command = getConnectorStatusCommand(connectorType);
        if (command == null) return "잘못된 커넥터 타입입니다. (source 또는 sink)";

        String result = executeCommand(host, command);

        if (result.trim().isEmpty()) {
            return "커넥터 상태를 가져오는 데 실패했습니다.";
        } else {
            return "커넥터 상태:\n" + result;
        }
    }

    private String getHost(int id) {
        return switch (id) {
            case 1 -> CONNECT_1_HOST;
            case 2 -> CONNECT_2_HOST;
            default -> null;
        };
    }

    private String getConnectorStatusCommand(String connectorType) {
        // 커넥터 타입에 따라 적절한 명령어를 반환
        return switch (connectorType) {
            case "source" -> CONNECT_SOURCE_STATUS_COMMAND;
            case "sink" -> CONNECT_SINK_STATUS_COMMAND;
            default -> null;
        };
    }

    private String executeCommand(String host, String command) {
        if (host == null) return "커넥트는 1 ~ 2번까지 있습니다.";

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

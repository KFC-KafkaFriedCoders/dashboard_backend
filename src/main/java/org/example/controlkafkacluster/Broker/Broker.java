package org.example.controlkafkacluster.Broker;

import io.github.cdimascio.dotenv.Dotenv;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/broker")
public class Broker {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String SSH_USER = dotenv.get("SSH_USER");
    private static final String SSH_PRIVATE_KEY = dotenv.get("SSH_PRIVATE_KEY");
    private static final String BROKER_1_HOST = dotenv.get("BROKER_1_HOST");
    private static final String BROKER_2_HOST = dotenv.get("BROKER_2_HOST");
    private static final String BROKER_3_HOST = dotenv.get("BROKER_3_HOST");
    private static final String KAFKA_START_COMMAND = dotenv.get("BROKER_START_COMMAND");
    private static final String KAFKA_STOP_COMMAND = dotenv.get("BROKER_STOP_COMMAND");
    private static final String KAFKA_STATUS_COMMAND = dotenv.get("BROKER_STATUS_COMMAND");
    private static final String KAFKA_PARTITION_COMMAND = dotenv.get("BROKER_PARTITION_COMMAND");

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
        if (host == null) return "브로커는 1 ~ 3번까지 있습니다.";

        String result = executeCommand(host, KAFKA_STATUS_COMMAND);

        String filteredResult = filterResult(result, "9092");

        if (filteredResult.trim().isEmpty()) {
            return "Kafka Broker " + host + " 는 현재 실행 중이지 않습니다.";
        } else {
            return "Kafka Broker " + host + " 는 정상적으로 실행 중입니다.\n" + filteredResult;
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

    @PostMapping("/partition/{id}")
    public String partitionInfo(@PathVariable int id) {
        String host = getHost(id);
        if (host == null) return "브로커는 1 ~ 3번까지 있습니다.";

        // 디렉토리 내 파일 목록을 보고, kafka-topics 실행
        String command = "bash -c 'cd /engn/confluent/bin && ./kafka-topics --describe'";

        String result = executeCommand(host, command);
        return "Kafka Broker " + host + "\n\n디렉토리 확인 및 파티션 정보:\n" + result;
    }

/*    private String parsePartitionInfo(String result) {
        StringBuilder partitionInfo = new StringBuilder();

        // 결과를 한 줄씩 확인하여 파티션 정보 추출
        for (String line : result.split("\n")) {
            if (line.contains("mysql-user")) {  // 특정 토픽에 대한 정보만 필터링
                String[] parts = line.split("\\s+");
                String partition = parts[0];
                String leader = parts[1];
                partitionInfo.append("파티션: ").append(partition)
                        .append(", 리더: ").append(leader).append("\n");
            }
        }
        return partitionInfo.toString();
    }*/

    private String getHost(int id) {
        return switch (id) {
            case 1 -> BROKER_1_HOST;
            case 2 -> BROKER_2_HOST;
            case 3 -> BROKER_3_HOST;
            default -> null;
        };
    }

    private String executeCommand(String host, String command) {
        if (host == null) return "브로커는 1 ~ 3번까지 있습니다.";

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

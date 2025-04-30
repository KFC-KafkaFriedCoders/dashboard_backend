package org.example.controlkafkacluster.Control;

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
@RequestMapping("/control")
public class Control {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String SSH_USER = dotenv.get("SSH_USER");
    private static final String SSH_PRIVATE_KEY = dotenv.get("SSH_PRIVATE_KEY");
    private static final String Control_1_HOST = dotenv.get("CONTROL_1_HOST");
    private static final String Control_2_HOST = dotenv.get("CONTROL_2_HOST");
    private static final String Control_3_HOST = dotenv.get("CONTROL_3_HOST");
    private static final String KAFKA_STATUS_COMMAND = dotenv.get("CONTROL_STATUS_COMMAND");

    @PostMapping("/start/{id}")
    public String start(@PathVariable int id) {
        return executeCommand(getHost(id), getStartCommand(id));
    }

    @PostMapping("/shutdown/{id}")
    public String shutdown(@PathVariable int id) {
        return executeCommand(getHost(id), getStopCommand(id));
    }

    @PostMapping("/status/{id}")
    public boolean status(@PathVariable int id) {
        String host = getHost(id);
        if (host == null) return false;

        String result = executeCommand(host, KAFKA_STATUS_COMMAND);
        String filteredResult = filterResult(result, "9093");

        return !filteredResult.trim().isEmpty();
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



    private String getHost(int id) {
        return switch (id) {
            case 1 -> Control_1_HOST;
            case 2 -> Control_2_HOST;
            case 3 -> Control_3_HOST;
            default -> null;
        };
    }

    private String getStartCommand(int id) {
        return switch (id) {
            case 1 -> dotenv.get("CONTROL_1_START_COMMAND");
            case 2 -> dotenv.get("CONTROL_2_START_COMMAND");
            case 3 -> dotenv.get("CONTROL_3_START_COMMAND");
            default -> null;
        };
    }

    private String getStopCommand(int id) {
        return switch (id) {
            case 1 -> dotenv.get("CONTROL_1_STOP_COMMAND");
            case 2 -> dotenv.get("CONTROL_2_STOP_COMMAND");
            case 3 -> dotenv.get("CONTROL_3_STOP_COMMAND");
            default -> null;
        };
    }

    private String executeCommand(String host, String command) {
        if (host == null || command == null) return "컨트롤러는 1 ~ 3번까지 있습니다.";

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

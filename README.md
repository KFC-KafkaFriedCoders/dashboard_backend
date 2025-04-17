# Kafka Status API

이 프로젝트는 AWS 환경에서 EC2 13개, RDS 2개를 사용한 Kafka Cluster 환경에서
Kafka 브로커, 컨트롤러, 커넥트, 스키마 레지스트리, 컨트롤 센터의 상태를 실시간으로 확인하고 관리할 수 있는 API입니다.

## 환경 설정 (Environment Setup)

이 프로젝트를 실행하기 위해서는 몇 가지 환경 설정이 필요합니다. 아래의 단계를 따라 설정해주세요.

### 1. 필수 의존성 (Dependencies)
프로젝트를 실행하기 전에 필요한 의존성들을 설치해야 합니다.

Java 17 이상: 이 프로젝트는 Java 17 이상에서 실행됩니다.

gradle: gradle을 사용하여 프로젝트 빌드 및 의존성을 관리합니다.

Kafka: Kafka 서버와 연결되어야 합니다. Kafka 클러스터가 정상적으로 동작하고 있어야 합니다.


### 2. `.env` 파일 설정

프로젝트 루트 디렉토리에 `.env` 파일을 생성하고, 필요한 환경 변수들을 추가합니다. 아래는 예시입니다:

```bash
# SSH 관련 설정
SSH_USER=본인 ec2 이름
SSH_PRIVATE_KEY=.pem 파일의 위치

# Kafka 브로커 호스트
BROKER_1_HOST, BROKER_2_HOST, BROKER_3_HOST

# Kafka 컨트롤러 호스트
CONTROL_1_HOST, CONTROL_2_HOST, CONTROL_3_HOST

# Kafka 스키마 레지스트리 호스트
SCHEMA_1_HOST, SCHEMA_2_HOST

# Kafka 커넥트 호스트
CONNECT_1_HOST, CONNECT_2_HOST

# KSQL 호스트
KSQL_1_HOST, KSQL_2_HOST

# Kafka Control Center 호스트
C3_1_HOST

# Kafka 브로커 명령어
BROKER_START_COMMAND, BROKER_STOP_COMMAND, BROKER_STATUS_COMMAND

# Kafka 커넥트 명령어
CONNECT_START_COMMAND, CONNECT_STOP_COMMAND, CONNECT_STATUS_COMMAND, CONNECT_SOURCE_STATUS_COMMAND, CONNECT_SINK_STATUS_COMMAND

# Kafka 스키마 레지스트리 명령어
SCHEMA_START_COMMAND, SCHEMA_STOP_COMMAND, SCHEMA_STATUS_COMMAND

# Kafka 컨트롤러 명령어
CONTROL_1_START_COMMAND, CONTROL_2_START_COMMAND, CONTROL_3_START_COMMAND
CONTROL_1_STOP_COMMAND, CONTROL_2_STOP_COMMAND, CONTROL_3_STOP_COMMAND
CONTROL_STATUS_COMMAND

# Kafka Control Center 명령어
C3_START_COMMAND, C3_STOP_COMMAND, C3_STATUS_COMMAND
```

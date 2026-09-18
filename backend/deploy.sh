#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ENV="${2:-${SPRING_PROFILES_ACTIVE:-}}"
case "${APP_ENV}" in
  ""|test1|test2|prod) ;;
  *) printf '不支持的环境：%s（仅支持 test1、test2、prod）\n' "${APP_ENV}" >&2; exit 2 ;;
esac
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/deployment${APP_ENV:+.${APP_ENV}}.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

if [[ -n "${APP_ENV}" ]]; then
  export SPRING_PROFILES_ACTIVE="${APP_ENV}"
fi

APP_NAME="${APP_NAME:-knowledge-ticket-api}"
APP_HOME="${APP_HOME:-${SCRIPT_DIR}/runtime${APP_ENV:+/${APP_ENV}}}"
SOURCE_JAR="${SOURCE_JAR:-${SCRIPT_DIR}/target/knowledge-ticket-api.jar}"
# 配置中的相对路径统一相对 deploy.sh 所在目录，而不是调用者当前目录。
[[ "${APP_HOME}" = /* ]] || APP_HOME="${SCRIPT_DIR}/${APP_HOME}"
[[ "${SOURCE_JAR}" = /* ]] || SOURCE_JAR="${SCRIPT_DIR}/${SOURCE_JAR}"
RUNTIME_JAR="${APP_HOME}/${APP_NAME}.jar"
PID_FILE="${APP_HOME}/${APP_NAME}.pid"
LOG_PATH="${LOG_PATH:-${APP_HOME}/logs}"
SERVER_PORT="${SERVER_PORT:-8080}"
BOOT_LOG="${LOG_PATH}/bootstrap.log"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-40}"
# 从 deployment.<环境>.env 读取 JAVA_HOME；未填写时使用 PATH 中的 java。
JAVA_BIN="${JAVA_HOME:+${JAVA_HOME}/bin/java}"
JAVA_BIN="${JAVA_BIN:-java}"

say(){ printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail(){ say "错误：$*" >&2; exit 1; }
pid_value(){
  local pid
  [[ -f "${PID_FILE}" ]] || return 0
  pid="$(<"${PID_FILE}")"
  [[ "${pid}" =~ ^[1-9][0-9]*$ && "${pid}" != 1 ]] && printf '%s' "${pid}"
  return 0
}
is_running(){
  local pid command state
  pid="$(pid_value)"
  [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null || return 1
  state="$(ps -p "${pid}" -o stat= 2>/dev/null)" || return 1
  [[ "${state}" != *Z* ]] || return 1
  command="$(ps -ww -p "${pid}" -o command= 2>/dev/null)" || return 1
  [[ " ${command} " == *" -jar ${RUNTIME_JAR} "* ]]
}

check_java(){
  command -v "${JAVA_BIN}" >/dev/null 2>&1 || fail "未找到 Java：${JAVA_BIN}，请在 ${ENV_FILE} 中配置 JAVA_HOME"
  local version
  version="$("${JAVA_BIN}" -version 2>&1 | awk -F '"' '/version/{print $2}')"
  [[ "${version}" == 1.8.* || "${version}" == 8.* ]] || fail "需要 JDK 8，当前版本为 ${version:-未知}"
  say "Java 版本检查通过：${version}，命令：${JAVA_BIN}"
}

check_environment(){
  [[ -z "${APP_ENV}" ]] && return 0
  [[ -n "${NUZAR_DB_PASSWORD:-}" ]] || fail "请在 ${ENV_FILE} 或环境变量中设置 NUZAR_DB_PASSWORD"
  [[ -n "${NUZAR_DB_URL:-}" ]] || fail "请配置 ${APP_ENV} 的 NUZAR_DB_URL"
  [[ -n "${NUZAR_DB_USERNAME:-}" ]] || fail "请配置 ${APP_ENV} 的 NUZAR_DB_USERNAME"
}

stop_app(){
  if ! is_running; then
    say "未找到匹配的运行进程，环境=${APP_ENV:-default}，PID文件=${PID_FILE}；请确认命令携带了正确环境名"
    if [[ -n "$(pid_value)" ]] && kill -0 "$(pid_value)" 2>/dev/null; then
      fail "PID 存在但进程身份不匹配，拒绝停止，请人工核对"
    fi
    return 0
  fi
  local pid wait_count=0
  pid="$(pid_value)"
  say "正在停止进程 ${pid}"
  kill -TERM "${pid}"
  while is_running && (( wait_count < 20 )); do sleep 1; wait_count=$((wait_count+1)); done
  is_running && fail "进程 ${pid} 未在 20 秒内退出，请人工确认后再部署"
  mv "${PID_FILE}" "${PID_FILE}.stopped.$(date '+%Y%m%d%H%M%S')"
  say "服务已停止"
}

ensure_runtime_jar(){
  [[ -f "${RUNTIME_JAR}" ]] && return 0
  [[ -f "${SOURCE_JAR}" ]] || fail "首次启动找不到源 JAR：${SOURCE_JAR}；请放入该文件或在 deployment 中配置 SOURCE_JAR"
  mkdir -p "${APP_HOME}"
  cp -p "${SOURCE_JAR}" "${RUNTIME_JAR}.new"
  mv "${RUNTIME_JAR}.new" "${RUNTIME_JAR}"
  say "首次启动：已自动部署到 ${RUNTIME_JAR}"
}

start_app(){
  check_environment
  is_running && fail "服务已在运行，PID=$(pid_value)"
  ensure_runtime_jar
  mkdir -p "${LOG_PATH}"
  [[ -f "${BOOT_LOG}" ]] && mv "${BOOT_LOG}" "${BOOT_LOG}.$(date '+%Y%m%d%H%M%S')"
  say "正在启动 ${APP_NAME}，环境 ${APP_ENV:-default}，端口 ${SERVER_PORT}"
  nohup "${JAVA_BIN}" ${JAVA_OPTS:--Xms256m -Xmx512m} -jar "${RUNTIME_JAR}" --server.port="${SERVER_PORT}" >"${BOOT_LOG}" 2>&1 &
  local pid=$!
  printf '%s\n' "${pid}" > "${PID_FILE}"
  local elapsed=0
  while (( elapsed < HEALTH_TIMEOUT )); do
    if ! kill -0 "${pid}" 2>/dev/null; then
      tail -n 80 "${BOOT_LOG}" >&2 || true
      fail "服务启动失败，详情见 ${BOOT_LOG}"
    fi
    if curl -fsS "http://127.0.0.1:${SERVER_PORT}/api/health" >/dev/null 2>&1; then
      say "部署成功，PID=${pid}"
      say "健康检查：http://127.0.0.1:${SERVER_PORT}/api/health"
      say "Swagger：http://127.0.0.1:${SERVER_PORT}/swagger-ui.html"
      return 0
    fi
    sleep 1; elapsed=$((elapsed+1))
  done
  tail -n 80 "${BOOT_LOG}" >&2 || true
  fail "服务在 ${HEALTH_TIMEOUT} 秒内未通过健康检查"
}

deploy_app(){
  check_java
  check_environment
  [[ -f "${SOURCE_JAR}" ]] || fail "待部署 JAR 不存在：${SOURCE_JAR}"
  mkdir -p "${APP_HOME}" "${LOG_PATH}" "${APP_HOME}/backup"
  stop_app
  if [[ -f "${RUNTIME_JAR}" ]]; then
    cp -p "${RUNTIME_JAR}" "${APP_HOME}/backup/${APP_NAME}.$(date '+%Y%m%d%H%M%S').jar"
    say "旧版本已备份"
  fi
  cp -p "${SOURCE_JAR}" "${RUNTIME_JAR}.new"
  mv "${RUNTIME_JAR}.new" "${RUNTIME_JAR}"
  start_app
}

status_app(){ if is_running; then say "服务运行中，环境=${APP_ENV:-default}，PID=$(pid_value)，端口=${SERVER_PORT}"; else say "未找到匹配进程，环境=${APP_ENV:-default}，PID文件=${PID_FILE}；请确认环境名和PID文件，勿据此认定其他环境已停止"; return 1; fi; }
show_logs(){ [[ -f "${LOG_PATH}/${APP_NAME}.log" ]] && tail -n 200 -f "${LOG_PATH}/${APP_NAME}.log" || tail -n 200 -f "${BOOT_LOG}"; }

case "${1:-deploy}" in
  deploy) deploy_app ;;
  start) check_java; start_app ;;
  stop) stop_app ;;
  restart) check_java; check_environment; stop_app; start_app ;;
  status) status_app ;;
  logs) show_logs ;;
  *) echo "用法：$0 {deploy|start|stop|restart|status|logs} [test1|test2|prod]"; exit 2 ;;
esac

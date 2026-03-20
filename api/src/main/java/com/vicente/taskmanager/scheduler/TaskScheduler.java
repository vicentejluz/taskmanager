package com.vicente.taskmanager.scheduler;

import com.vicente.taskmanager.service.TaskSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component(value = "TaskMaintenanceScheduler")
public class TaskScheduler {
    private final TaskSchedulerService taskSchedulerService;
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    /**
     * ================================================================
     * CONTROLE DE CONCORRÊNCIA COM AtomicBoolean
     * ================================================================
     * *
     * Este AtomicBoolean é utilizado como um lock (trava) em memória
     * para evitar execuções concorrentes do scheduler.
     * *
     * Por que isso é necessário?
     * ------------------------------------------------
     * Este scheduler pode ser acionado por duas fontes diferentes:
     * *
     *  1) Inicialização da aplicação (ApplicationReadyEvent)
     *  2) Execução agendada (@Scheduled)
     * *
     * Essas execuções podem ocorrer quase ao mesmo tempo e
     * são processadas por threads diferentes.
     * *
     * Sem um mecanismo de controle de concorrência, a mesma
     * lógica de manutenção poderia ser executada simultaneamente,
     * causando:
     *  - atualizações duplicadas
     *  - exclusões duplicadas
     *  - condições de corrida (race conditions)
     *  - inconsistência no estado do banco de dados
     * *
     * Por que não usar um boolean simples?
     * ------------------------------------------------
     * Um boolean comum NÃO é thread-safe.
     * Duas threads podem ler o valor 'false' ao mesmo tempo
     * e ambas iniciarem a execução, resultando em execuções
     * concorrentes.
     * *
     * Por que AtomicBoolean?
     * ------------------------------------------------
     * AtomicBoolean fornece operações atômicas (thread-safe).
     * O método compareAndSet(valorEsperado, novoValor) é executado
     * como uma única operação atômica pela JVM/CPU.
     * *
     * compareAndSet(false, true) significa:
     *  "Se o valor atual for false, altere para true e retorne true.
     *   Caso contrário, não altere nada e retorne false."
     * *
     * Isso garante que apenas UMA thread consiga adquirir o lock.
     * *
     * Como o fluxo de execução funciona:
     * ------------------------------------------------
     * 1) Estado inicial:
     *    running = false
     * *
     * 2) Primeira thread entra:
     *    compareAndSet(false, true) -> true
     *    Execução iniciada
     * *
     * 3) Segunda thread entra enquanto a primeira está executando:
     *    compareAndSet(false, true) -> false
     *    Execução ignorada
     * *
     * 4) Após o término da execução (com sucesso ou erro):
     *    running é resetado para false dentro do bloco finally,
     *    liberando o lock.
     * *
     * Observações importantes:
     * ------------------------------------------------
     * - Este lock funciona APENAS dentro da mesma JVM.
     * - NÃO protege contra execuções concorrentes em
     *   múltiplas instâncias da aplicação (cluster).
     * - Para ambientes distribuídos, é necessário usar
     *   um lock distribuído (banco de dados, Redis,
     *   ShedLock, Quartz, etc).
     * *
     * Esta abordagem é leve, não bloqueante e ideal
     * para schedulers e jobs de manutenção executados
     * em uma única instância da aplicação.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TaskScheduler(TaskSchedulerService taskSchedulerService) {
        this.taskSchedulerService = taskSchedulerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        execute("TASK STARTUP");
    }
    
    @Scheduled(cron = "${spring.task.scheduling.cron}")
    public void runScheduled() {
        execute("TASK SCHEDULER");
    }


    private void execute(String source) {
        if (!running.compareAndSet(false, true)) {
            logger.warn("[{}] Execution skipped — already running", source);
            return;
        }

        logger.info("[{}] Running task maintenance", source);

        long start = System.currentTimeMillis();
        try {
            taskSchedulerService.updateOverdueTasks(source);
            taskSchedulerService.deleteCancelledTasksOlderThan90Days(source);
            taskSchedulerService.deleteDoneTasksOlderThan180Days(source);

            long duration = System.currentTimeMillis() - start;
            logger.info("[{}] Task maintenance scheduler finished | duration={}ms", source, duration);

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            logger.warn("[{}] Task skipped due to concurrent update | reason=optimistic_lock", source);
        } catch (Exception e){
            logger.error("[{}] Task maintenance scheduler failed due to unexpected error", source, e);
        }finally {
            running.set(false);
        }
    }
}

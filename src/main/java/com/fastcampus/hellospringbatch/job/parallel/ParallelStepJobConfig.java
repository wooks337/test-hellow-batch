package com.fastcampus.hellospringbatch.job.parallel;

import com.fastcampus.hellospringbatch.dto.AmountDto;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * 단일 프로세스 멀티 쓰레드에서 Flow를 사용해 스텝을 동시에 실행한다.
 * Step 자체를 병렬 처리 한다고 생각하면됨.
 *
 *
 * 1 Step 에서  thread 1 번
 * 2 Step 에서는 thread 2 번으로 동작되는것을 볼 수 있다.
 *
 */
@Configuration
@AllArgsConstructor
public class ParallelStepJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job parallelJob(Flow splitFlow) {
        return jobBuilderFactory.get("parallelJob")
                .incrementer(new RunIdIncrementer())
                .start(splitFlow)
                .build()
                .build();
    }

    /**
     * Flow 를 이용해서 병렬 처리함 (flow 안에 여러 flow를 둔다.)
     * @param taskExecutor
     * @param flowAmountFileStep
     * @param flowAnotherStep
     * @return Flow
     */
    @Bean
    public Flow splitFlow(TaskExecutor taskExecutor,
                          Flow flowAmountFileStep,
                          Flow flowAnotherStep) {
        return new FlowBuilder<SimpleFlow>("parallelFlow")
                .split(taskExecutor)
                .add(flowAmountFileStep, flowAnotherStep)
                .build();
    }

    @Bean
    public Flow flowAmountFileStep(Step amountFileStep) {
        return new FlowBuilder<SimpleFlow>("flowAmountFileStep")
                .start(amountFileStep)
                .build();
    }

    @Bean
    public Step amountFileStep(FlatFileItemReader<AmountDto> amountFileItemReader,
                                ItemProcessor<AmountDto, AmountDto> amountFileItemProcessor,
                                FlatFileItemWriter<AmountDto> amountFileItemWriter
    ) {
        return stepBuilderFactory.get("multiThreadStep")
                .<AmountDto, AmountDto>chunk(10)
                .reader(amountFileItemReader)
                .processor(amountFileItemProcessor)
                .writer(amountFileItemWriter)
                .build();
    }

    @Bean
    public Flow flowAnotherStep(Step anotherStep) {
        return new FlowBuilder<SimpleFlow>("anotherStep")
                .start(anotherStep)
                .build();
    }

    @Bean
    public Step anotherStep() {
        return stepBuilderFactory.get("anotherStep")
                .tasklet((contribution, chunkContext) -> {
                    Thread.sleep(500);
                    System.out.println("Another Step Completed. Thread = " + Thread.currentThread().getName());
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}

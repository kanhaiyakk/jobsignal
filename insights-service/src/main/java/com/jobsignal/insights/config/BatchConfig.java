package com.jobsignal.insights.config;

import com.jobsignal.insights.service.InsightAggregationTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Job weeklyReportJob(JobRepository jobRepository, Step aggregationStep) {
        return new JobBuilder("weeklyReportJob", jobRepository)
                .start(aggregationStep)
                .build();
    }

    @Bean
    public Step aggregationStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                InsightAggregationTasklet tasklet) {
        return new StepBuilder("aggregationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}

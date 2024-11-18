/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.quartz;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.internal.quartz.JobGenerator;

/**
 * REST API over {@link Scheduler} operations.
 *
 * @author edgar
 * @since 2.5.1
 */
public class QuartzApp extends Jooby {

  private Scheduler scheduler;

  {
    get(
        "/",
        ctx -> {
          Scheduler scheduler = getScheduler();
          List<JobKey> jobKeys = new ArrayList<>();
          for (String group : scheduler.getJobGroupNames()) {
            jobKeys.addAll(scheduler.getJobKeys(GroupMatcher.groupEquals(group)));
          }
          return jobKeys.stream().map(key -> job(ctx, scheduler, key)).collect(Collectors.toList());
        });

    get(
        "/{group}/{name}",
        ctx -> {
          Scheduler scheduler = getScheduler();
          return job(ctx, scheduler, jobKey(ctx));
        });

    get(
        "/{group}/{name}/trigger",
        ctx -> {
          Scheduler scheduler = getScheduler();
          JobKey jobKey = jobKey(ctx);
          if (!isJobRunning(scheduler, jobKey)) {
            scheduler.triggerJob(jobKey, new JobDataMap(ctx.queryMap()));
          }
          return toMap(
              scheduler.getJobDetail(jobKey), scheduler.getTriggersOfJob(jobKey), zoneId(ctx));
        });

    get(
        "/{group}/{name}/interrupt",
        ctx -> {
          Scheduler scheduler = getScheduler();
          JobKey jobKey = jobKey(ctx);
          for (JobExecutionContext jec : scheduler.getCurrentlyExecutingJobs()) {
            JobKey currentKey = jec.getJobDetail().getKey();
            if (currentKey.equals(jobKey)) {
              if (jec.getJobInstance() instanceof InterruptableJob) {
                ((InterruptableJob) jec.getJobInstance()).interrupt();
              }
            }
          }

          return toMap(
              scheduler.getJobDetail(jobKey), scheduler.getTriggersOfJob(jobKey), zoneId(ctx));
        });

    get(
        "/{group}/{name}/pause",
        ctx -> {
          Scheduler scheduler = getScheduler();
          JobKey jobKey = jobKey(ctx);
          scheduler.pauseJob(jobKey);
          return toMap(
              scheduler.getJobDetail(jobKey), scheduler.getTriggersOfJob(jobKey), zoneId(ctx));
        });

    get(
        "/{group}/{name}/resume",
        ctx -> {
          Scheduler scheduler = getScheduler();
          JobKey jobKey = jobKey(ctx);
          scheduler.resumeJob(jobKey);
          return toMap(
              scheduler.getJobDetail(jobKey), scheduler.getTriggersOfJob(jobKey), zoneId(ctx));
        });

    get(
        "/{group}/{name}/reschedule",
        ctx -> {
          Scheduler scheduler = getScheduler();
          String value = ctx.query("trigger").value();
          JobKey jobKey = jobKey(ctx);
          Trigger trigger = JobGenerator.newTrigger(getConfig(), value, jobKey);
          scheduler.rescheduleJob(trigger.getKey(), trigger);
          return toMap(
              scheduler.getJobDetail(jobKey), scheduler.getTriggersOfJob(jobKey), zoneId(ctx));
        });

    /** Delete a job and their associated trigger(s). */
    delete(
        "/{group}/{name}",
        ctx -> {
          var scheduler = getScheduler();
          var jobKey = jobKey(ctx);
          var deleted = scheduler.deleteJob(jobKey);
          return Map.of("key", jobKey.toString(), "deleted", deleted);
        });
  }

  private Map<String, Object> job(Context ctx, Scheduler scheduler, JobKey jobKey) {
    try {
      Map<String, Object> job =
          toMap(scheduler.getJobDetail(jobKey), scheduler.getTriggersOfJob(jobKey), zoneId(ctx));
      job.put("actions", actions(ctx, jobKey));
      return job;
    } catch (SchedulerException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private ZoneId zoneId(Context ctx) {
    return ctx.query("zoneId").toOptional(ZoneId.class).orElse(ZoneId.of("UTC"));
  }

  private JobKey jobKey(Context ctx) {
    return JobKey.jobKey(ctx.path("name").value(), ctx.path("group").value());
  }

  private Map<String, Object> toMap(
      JobDetail detail, List<? extends Trigger> triggers, ZoneId zoneId) throws SchedulerException {

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("key", detail.getKey().toString());
    Optional.ofNullable(detail.getDescription()).ifPresent(value -> json.put("description", value));
    json.put("jobDataMap", detail.getJobDataMap());
    json.put("stoppable", InterruptableJob.class.isAssignableFrom(detail.getJobClass()));
    json.put("concurrentExecutionDisallowed", detail.isConcurrentExecutionDisallowed());
    json.put("durable", detail.isDurable());
    json.put("persistJobDataAfterExecution", detail.isPersistJobDataAfterExecution());
    json.put("requestsRecovery", detail.requestsRecovery());
    json.put("triggers", toJson(triggers, zoneId));

    return json;
  }

  private Map<String, String> actions(Context ctx, JobKey key) {
    Map<String, String> actions = new LinkedHashMap<>();
    Route route = ctx.getRoute();
    String path;
    String base;
    if (route.getPathKeys().isEmpty()) {
      base = route.getPattern();
      path = route.getPattern() + "/" + key.getGroup() + "/" + key.getName();
    } else {
      base = route.getPattern().substring(0, route.getPattern().indexOf('{') - 1);
      path = route.reverse(Map.of("group", key.getGroup(), "name", key.getName()));
    }
    actions.put("base", base);
    actions.put("detail", path);
    actions.put("trigger", path + "/trigger");
    actions.put("interrupt", path + "/interrupt");
    actions.put("pause", path + "/pause");
    actions.put("resume", path + "/resume");
    actions.put("reschedule", path + "/reschedule");
    return actions;
  }

  private List<Map<String, Object>> toJson(List<? extends Trigger> triggers, ZoneId zoneId)
      throws SchedulerException {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Trigger trigger : triggers) {
      result.add(toJson(trigger, zoneId));
    }
    return result;
  }

  private Map<String, Object> toJson(Trigger trigger, ZoneId zoneId) throws SchedulerException {
    Scheduler scheduler = getScheduler();
    Trigger.TriggerState state = scheduler.getTriggerState(trigger.getKey());

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("key", trigger.getKey().toString());
    json.put("state", state);
    json.put("priority", trigger.getPriority());
    json.put("zoneId", zoneId);
    Optional.ofNullable(trigger.getCalendarName())
        .ifPresent(value -> json.put("calendarName", value));
    Optional.ofNullable(trigger.getDescription())
        .ifPresent(value -> json.put("description", value));

    json.put("startTime", format(trigger.getStartTime(), zoneId));
    json.put("endTime", format(trigger.getEndTime(), zoneId));
    json.put("finalFireTime", format(trigger.getFinalFireTime(), zoneId));
    json.put("nextFireTime", format(trigger.getNextFireTime(), zoneId));
    json.put("previousFireTime", format(trigger.getPreviousFireTime(), zoneId));

    json.put("misfireInstruction", trigger.getMisfireInstruction());

    json.put("mayFireAgain", trigger.mayFireAgain());
    if (trigger instanceof CronTrigger) {
      json.put("cron", ((CronTrigger) trigger).getCronExpression());
    } else if (trigger instanceof SimpleTrigger) {
      json.put("repeatCount", ((SimpleTrigger) trigger).getRepeatCount());
      json.put("repeatInterval", ((SimpleTrigger) trigger).getRepeatInterval());
    } else if (trigger instanceof CalendarIntervalTrigger) {
      json.put("repeatInterval", ((CalendarIntervalTrigger) trigger).getRepeatInterval());
      json.put("repeatIntervalUnit", ((CalendarIntervalTrigger) trigger).getRepeatIntervalUnit());
    } else if (trigger instanceof DailyTimeIntervalTrigger) {
      json.put("repeatInterval", ((DailyTimeIntervalTrigger) trigger).getRepeatInterval());
      json.put("repeatIntervalUnit", ((DailyTimeIntervalTrigger) trigger).getRepeatIntervalUnit());
    }

    json.put("jobDataMap", trigger.getJobDataMap());

    return json;
  }

  private Object format(Date dateTime, ZoneId zoneId) {
    return dateTime == null
        ? null
        : ISO_OFFSET_DATE_TIME.withZone(zoneId).format(dateTime.toInstant());
  }

  private boolean isJobRunning(final Scheduler scheduler, final JobKey job)
      throws SchedulerException {
    return runningJob(scheduler, job).isPresent();
  }

  private Optional<JobExecutionContext> runningJob(final Scheduler scheduler, final JobKey job)
      throws SchedulerException {
    List<JobExecutionContext> jobs = scheduler.getCurrentlyExecutingJobs();
    return jobs.stream().filter(it -> it.getJobDetail().getKey().equals(job)).findFirst();
  }

  private Scheduler getScheduler() {
    if (scheduler == null) {
      this.scheduler = require(Scheduler.class);
    }
    return scheduler;
  }

  /**
   * New quartz app.
   *
   * @param scheduler Scheduler to use.
   */
  public QuartzApp(@Nullable Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  /** New quartz app. */
  public QuartzApp() {}
}

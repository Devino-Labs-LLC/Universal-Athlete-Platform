import { useEffect, useRef, useState } from 'react';
import {
  Controller,
  useFieldArray,
  useWatch,
  type Control,
  type FieldErrors,
  type UseFormRegister,
  type UseFormSetValue,
} from 'react-hook-form';

import { Button } from '@/core/components/Button';
import { RatingScale } from '@/features/recovery/components/RatingScale';
import type { CreateCheckInFormValues } from '@/features/recovery/models/checkInForm';
import {
  BODY_AREAS,
  BODY_SIDES,
  bodyAreaLabel,
  bodySideLabel,
  type RecoveryRatingMetric,
} from '@/features/recovery/models/labels';
import { minutesToHoursMinutes, parseSleepDurationFields } from '@/features/recovery/utils/sleepDuration';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import styles from '@/features/recovery/forms/RecoveryCheckInForm.module.scss';

const REQUIRED_RATINGS: RecoveryRatingMetric[] = [
  'fatigue',
  'muscleSoreness',
  'stress',
  'mood',
  'motivation',
];

const RATING_VALUES = [1, 2, 3, 4, 5] as const;

interface RecoveryCheckInFormProps {
  control: Control<CreateCheckInFormValues>;
  register: UseFormRegister<CreateCheckInFormValues>;
  errors: FieldErrors<CreateCheckInFormValues>;
  setValue: UseFormSetValue<CreateCheckInFormValues>;
}

function initialSleepText(totalMinutes?: number): { hours: string; minutes: string } {
  if (totalMinutes == null) {
    return { hours: '', minutes: '' };
  }
  const split = minutesToHoursMinutes(totalMinutes);
  return { hours: String(split.hours), minutes: String(split.minutes) };
}

export function RecoveryCheckInForm({
  control,
  register,
  errors,
  setValue,
}: RecoveryCheckInFormProps) {
  const discomfort = useFieldArray({ control, name: 'discomfortAreas' });
  const sleepDurationMinutes = useWatch({ control, name: 'sleepDurationMinutes' });
  const initial = initialSleepText(sleepDurationMinutes);
  const [hoursText, setHoursText] = useState(initial.hours);
  const [minutesText, setMinutesText] = useState(initial.minutes);
  const lastEmitted = useRef(sleepDurationMinutes);

  useEffect(() => {
    if (sleepDurationMinutes === lastEmitted.current) {
      return;
    }
    lastEmitted.current = sleepDurationMinutes;
    const next = initialSleepText(sleepDurationMinutes);
    setHoursText(next.hours);
    setMinutesText(next.minutes);
  }, [sleepDurationMinutes]);

  const handleSleepChange = (nextHours: string, nextMinutes: string) => {
    setHoursText(nextHours);
    setMinutesText(nextMinutes);
    const parsed = parseSleepDurationFields(nextHours, nextMinutes);
    lastEmitted.current = parsed;
    setValue('sleepDurationMinutes', parsed, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  return (
    <div className={surfaces.hub} data-testid="recovery-check-in-form">
      <section className={surfaces.panel} aria-labelledby="check-in-ratings-heading">
        <div className={surfaces.panelHeader}>
          <h2 className={surfaces.panelTitle} id="check-in-ratings-heading">
            How you feel
          </h2>
          <span className={surfaces.panelHint}>Required · 1–5</span>
        </div>
        <div className={styles.ratings}>
          {REQUIRED_RATINGS.map((metric) => (
            <Controller
              key={metric}
              control={control}
              name={metric}
              render={({ field, fieldState }) => (
                <RatingScale
                  metric={metric}
                  value={field.value}
                  onChange={field.onChange}
                  error={fieldState.error?.message}
                />
              )}
            />
          ))}
        </div>
      </section>

      <section className={surfaces.panel} aria-labelledby="check-in-sleep-heading">
        <div className={surfaces.panelHeader}>
          <h2 className={surfaces.panelTitle} id="check-in-sleep-heading">
            Sleep
          </h2>
          <span className={surfaces.panelHint}>Optional · leave blank to omit</span>
        </div>
        <div className={styles.sleepGrid}>
          <div className="field">
            <label className="label" htmlFor="sleep-hours">
              Hours
            </label>
            <input
              id="sleep-hours"
              type="number"
              min={0}
              inputMode="numeric"
              className="input"
              aria-describedby={errors.sleepDurationMinutes ? 'sleep-duration-error' : undefined}
              value={hoursText}
              onChange={(event) => handleSleepChange(event.target.value, minutesText)}
            />
          </div>
          <div className="field">
            <label className="label" htmlFor="sleep-minutes">
              Minutes
            </label>
            <input
              id="sleep-minutes"
              type="number"
              min={0}
              max={59}
              inputMode="numeric"
              className="input"
              aria-describedby={errors.sleepDurationMinutes ? 'sleep-duration-error' : undefined}
              value={minutesText}
              onChange={(event) => handleSleepChange(hoursText, event.target.value)}
            />
          </div>
        </div>
        {errors.sleepDurationMinutes ? (
          <p className="fieldError" id="sleep-duration-error">
            {errors.sleepDurationMinutes.message}
          </p>
        ) : null}
        <Controller
          control={control}
          name="sleepQuality"
          render={({ field, fieldState }) => (
            <RatingScale
              metric="sleepQuality"
              value={field.value}
              onChange={field.onChange}
              optional
              error={fieldState.error?.message}
            />
          )}
        />
      </section>

      <section className={surfaces.panel} aria-labelledby="check-in-discomfort-heading">
        <div className={surfaces.panelHeader}>
          <h2 className={surfaces.panelTitle} id="check-in-discomfort-heading">
            Discomfort
          </h2>
          <span className={surfaces.panelHint}>Optional</span>
        </div>
        {discomfort.fields.length === 0 ? (
          <p className={surfaces.metaText}>Add a body area only if something feels off today.</p>
        ) : null}
        {discomfort.fields.map((field, index) => (
          <div key={field.id} className={styles.discomfortCard}>
            <div className={styles.discomfortGrid}>
              <div className="field">
                <label className="label" htmlFor={`discomfort-area-${index}`}>
                  Body area {index + 1}
                </label>
                <select
                  id={`discomfort-area-${index}`}
                  className="input"
                  {...register(`discomfortAreas.${index}.bodyArea`)}
                >
                  {BODY_AREAS.map((area) => (
                    <option key={area} value={area}>
                      {bodyAreaLabel(area)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label className="label" htmlFor={`discomfort-side-${index}`}>
                  Side
                </label>
                <select
                  id={`discomfort-side-${index}`}
                  className="input"
                  {...register(`discomfortAreas.${index}.side`)}
                >
                  {BODY_SIDES.map((side) => (
                    <option key={side} value={side}>
                      {bodySideLabel(side)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label className="label" htmlFor={`discomfort-intensity-${index}`}>
                  Intensity
                </label>
                <select
                  id={`discomfort-intensity-${index}`}
                  className="input"
                  {...register(`discomfortAreas.${index}.intensity`, { valueAsNumber: true })}
                >
                  {RATING_VALUES.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label className="label" htmlFor={`discomfort-notes-${index}`}>
                  Notes
                </label>
                <input
                  id={`discomfort-notes-${index}`}
                  className="input"
                  maxLength={250}
                  {...register(`discomfortAreas.${index}.notes`)}
                />
              </div>
            </div>
            <Button type="button" variant="ghost" onClick={() => discomfort.remove(index)}>
              Remove area
            </Button>
          </div>
        ))}
        {errors.discomfortAreas?.message ? (
          <p className="fieldError">{errors.discomfortAreas.message}</p>
        ) : null}
        <Button
          type="button"
          variant="secondary"
          disabled={discomfort.fields.length >= 20}
          onClick={() =>
            discomfort.append({
              bodyArea: 'LOWER_BACK',
              side: 'CENTER',
              intensity: 2,
            })
          }
        >
          Add discomfort
        </Button>
      </section>

      <section className={surfaces.panel} aria-labelledby="check-in-notes-heading">
        <div className={surfaces.panelHeader}>
          <h2 className={surfaces.panelTitle} id="check-in-notes-heading">
            Notes
          </h2>
          <span className={surfaces.panelHint}>Optional · max 2000</span>
        </div>
        <div className="field">
          <label className="label" htmlFor="check-in-notes">
            Anything else
          </label>
          <textarea id="check-in-notes" className="input" rows={3} maxLength={2000} {...register('notes')} />
          {errors.notes ? <p className="fieldError">{errors.notes.message}</p> : null}
        </div>
      </section>
    </div>
  );
}

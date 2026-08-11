export const athleteQueryKeys = {
  all: ['athlete'] as const,
  profile: () => [...athleteQueryKeys.all, 'profile'] as const,
  sports: () => [...athleteQueryKeys.all, 'sports'] as const,
  goals: () => [...athleteQueryKeys.all, 'goals'] as const,
};

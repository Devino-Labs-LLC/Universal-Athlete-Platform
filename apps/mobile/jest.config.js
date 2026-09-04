/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-expo',
  testMatch: ['**/__tests__/**/*.(test|spec).(ts|tsx)'],
  moduleNameMapper: {
    '^@/src/app/config/(.*)$': '<rootDir>/src/config/$1',
    '^@/src/app/providers/(.*)$': '<rootDir>/src/providers/$1',
    '^@/src/app/theme/(.*)$': '<rootDir>/src/theme/$1',
    '^@/(.*)$': '<rootDir>/$1',
  },
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/**/*.d.ts'],
  coverageDirectory: 'coverage',
  coverageReporters: ['lcov', 'text-summary'],
};

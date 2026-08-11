export function formatVolumeKg(kg: number | string): string {
  const value = Number(kg);
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)} t`;
  }
  return `${Math.round(value)} kg`;
}

export function formatDistance(meters: number | string): string {
  const value = Number(meters);
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)} km`;
  }
  return `${Math.round(value)} m`;
}

export function formatDurationSeconds(seconds: number | string): string {
  const totalMinutes = Math.round(Number(seconds) / 60);
  if (totalMinutes < 60) {
    return `${totalMinutes} min`;
  }
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
}

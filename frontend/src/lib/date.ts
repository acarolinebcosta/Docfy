const DATE_TIME_FORMATTER = new Intl.DateTimeFormat(
  "pt-BR",
  {
    dateStyle: "short",
    timeStyle: "short",
  },
);

export function formatDateTime(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Data indisponível";
  }

  return DATE_TIME_FORMATTER.format(date);
}

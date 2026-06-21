export type RailGroup<T> = {
  key: string
  label: string
  items: T[]
}

export const buildRailGroup = <T>(key: string, label: string, items: T[]): RailGroup<T>[] => {
  return items.length ? [{key, label, items}] : []
}

export const groupByStatus = <T extends {status: string}>(
  items: T[],
  order: string[],
  labelForStatus: (status: string) => string
) => {
  const grouped = new Map<string, T[]>()

  for (const item of items) {
    const bucket = grouped.get(item.status) ?? []
    bucket.push(item)
    grouped.set(item.status, bucket)
  }

  return Array.from(grouped.entries())
    .sort((left, right) => {
      const leftIndex = order.indexOf(left[0])
      const rightIndex = order.indexOf(right[0])
      return (leftIndex === -1 ? order.length : leftIndex) - (rightIndex === -1 ? order.length : rightIndex)
    })
    .map(([status, groupedItems]) => ({
      key: `active-${status}`,
      label: labelForStatus(status),
      items: groupedItems
    }))
}

export const formatRailDateTime = (value: string | null | undefined) => {
  if (!value) {
    return "No scheduled time"
  }

  return new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value))
}

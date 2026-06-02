export function formatPrice(price: number): string {
  const absolutePrice = Math.abs(price)

  if (absolutePrice >= 1000) {
    return price.toFixed(2)
  }

  if (absolutePrice >= 1) {
    return price.toFixed(2)
  }

  if (absolutePrice >= 0.01) {
    return price.toFixed(4)
  }

  if (absolutePrice >= 0.0001) {
    return price.toFixed(6)
  }

  return price.toFixed(8)
}

export function formatOptionalPrice(price?: number | string | null): string {
  if (price === undefined || price === null) {
    return '--'
  }

  const numericPrice = Number(price)

  if (!Number.isFinite(numericPrice)) {
    return '--'
  }

  return formatPrice(numericPrice)
}

export function formatMoney(value?: number | string | null): string {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(numericValue)
}

export function formatSignedMoney(value?: number | string | null): string {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  const formattedValue = formatMoney(Math.abs(numericValue))

  if (numericValue > 0) {
    return `+${formattedValue}`
  }

  if (numericValue < 0) {
    return `-${formattedValue}`
  }

  return formattedValue
}

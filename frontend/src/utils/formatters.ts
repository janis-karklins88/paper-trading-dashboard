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

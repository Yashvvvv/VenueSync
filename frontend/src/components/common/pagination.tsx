"use client"

import { CaretLeft, CaretRight } from "@/components/icons"
import type { SpringBootPagination } from "@/domain/domain"

interface PaginationProps<T> {
  pagination: SpringBootPagination<T>
  onPageChange: (page: number) => void
}

export function Pagination<T>({ pagination, onPageChange }: PaginationProps<T>) {
  const { number: currentPage, totalPages, first, last } = pagination

  if (totalPages <= 1) return null

  const getPageNumbers = (): (number | "ellipsis")[] => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i)
    }

    const pages: (number | "ellipsis")[] = [0]

    if (currentPage > 3) pages.push("ellipsis")

    for (let i = Math.max(1, currentPage - 1); i <= Math.min(totalPages - 2, currentPage + 1); i++) {
      pages.push(i)
    }

    if (currentPage < totalPages - 4) pages.push("ellipsis")

    pages.push(totalPages - 1)

    return pages
  }

  const arrow =
    "focus-ring btn-press flex h-9 items-center gap-1.5 rounded-md border border-border px-3 text-sm text-muted-foreground transition-colors hover:border-foreground/30 hover:text-foreground disabled:pointer-events-none disabled:opacity-35"

  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-2">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={first}
        className={arrow}
        aria-label="Previous page"
      >
        <CaretLeft weight="bold" size={14} />
        <span className="hidden sm:inline">Previous</span>
      </button>

      <div className="flex gap-1">
        {getPageNumbers().map((page, index) =>
          page === "ellipsis" ? (
            <span
              key={`ellipsis-${index}`}
              className="flex h-9 w-6 items-center justify-center font-mono text-sm text-muted-foreground"
            >
              ...
            </span>
          ) : (
            <button
              key={page}
              onClick={() => onPageChange(page)}
              aria-current={page === currentPage ? "page" : undefined}
              className={`focus-ring btn-press h-9 w-9 rounded-md border font-mono text-sm tabular-nums transition-colors ${
                page === currentPage
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border text-muted-foreground hover:border-foreground/30 hover:text-foreground"
              }`}
            >
              {page + 1}
            </button>
          ),
        )}
      </div>

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={last}
        className={arrow}
        aria-label="Next page"
      >
        <span className="hidden sm:inline">Next</span>
        <CaretRight weight="bold" size={14} />
      </button>
    </nav>
  )
}

export default Pagination

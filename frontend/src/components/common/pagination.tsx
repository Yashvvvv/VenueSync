"use client"

import { ChevronLeft, ChevronRight, MoreHorizontal } from "lucide-react"
import { Button } from "../ui/button"
import type { SpringBootPagination } from "@/domain/domain"
import { motion } from "framer-motion"

interface PaginationProps<T> {
  pagination: SpringBootPagination<T>
  onPageChange: (page: number) => void
}

export function Pagination<T>({ pagination, onPageChange }: PaginationProps<T>) {
  const { number: currentPage, totalPages, first, last } = pagination

  if (totalPages <= 1) return null

  const getPageNumbers = () => {
    const pages: (number | "ellipsis")[] = []

    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i)
    }

    // Always show first page
    pages.push(0)

    if (currentPage > 3) {
      pages.push("ellipsis")
    }

    // Show pages around current page
    for (let i = Math.max(1, currentPage - 1); i <= Math.min(totalPages - 2, currentPage + 1); i++) {
      pages.push(i)
    }

    if (currentPage < totalPages - 4) {
      pages.push("ellipsis")
    }

    // Always show last page
    pages.push(totalPages - 1)

    return pages
  }

  const pages = getPageNumbers()

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex items-center justify-center gap-2"
    >
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={first}
        className="gap-1.5 glass border-border/50 hover:bg-primary/10 hover:border-primary/30 hover:text-primary disabled:opacity-40"
      >
        <ChevronLeft className="w-4 h-4" />
        <span className="hidden sm:inline">Previous</span>
      </Button>

      <div className="flex gap-1">
        {pages.map((page, index) =>
          page === "ellipsis" ? (
            <div key={`ellipsis-${index}`} className="w-9 h-9 flex items-center justify-center">
              <MoreHorizontal className="w-4 h-4 text-muted-foreground" />
            </div>
          ) : (
            <motion.div key={page} whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Button
                variant={page === currentPage ? "default" : "outline"}
                size="sm"
                onClick={() => onPageChange(page)}
                className={
                  page === currentPage
                    ? "gradient-primary text-white w-9 h-9 p-0 shadow-lg shadow-primary/20"
                    : "glass border-border/50 hover:bg-primary/10 hover:border-primary/30 hover:text-primary w-9 h-9 p-0"
                }
              >
                {page + 1}
              </Button>
            </motion.div>
          ),
        )}
      </div>

      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={last}
        className="gap-1.5 glass border-border/50 hover:bg-primary/10 hover:border-primary/30 hover:text-primary disabled:opacity-40"
      >
        <span className="hidden sm:inline">Next</span>
        <ChevronRight className="w-4 h-4" />
      </Button>
    </motion.div>
  )
}

export default Pagination

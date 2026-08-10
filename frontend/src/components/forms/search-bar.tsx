"use client"

import type React from "react"

import { Button } from "../ui/button"
import { AnimatePresence, motion, useReducedMotion } from "framer-motion"
import { useCallback, useId, useState } from "react"
import { MagnifyingGlass, X } from "@/components/icons"

interface SearchBarProps {
  value: string
  onChange: (value: string) => void
  onSearch: () => void
  placeholder?: string
  className?: string
  size?: "default" | "large"
  /** Rendered above the field. Visually hidden when the context already says it. */
  label?: string
  hideLabel?: boolean
}

/**
 * One bordered field with the action attached to its trailing edge.
 *
 * No blur glow behind the input on focus. The border changing to ember is
 * a clearer focus signal and it survives `prefers-reduced-transparency`.
 * The label exists in markup even when hidden, because placeholder text is
 * not a label.
 */
export const SearchBar: React.FC<SearchBarProps> = ({
  value,
  onChange,
  onSearch,
  placeholder = "Search events, venues or artists",
  className = "",
  size = "default",
  label = "Search events",
  hideLabel = true,
}) => {
  const [isFocused, setIsFocused] = useState(false)
  const reduce = useReducedMotion()
  const id = useId()

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter") onSearch()
    },
    [onSearch],
  )

  const handleClear = () => {
    onChange("")
    onSearch()
  }

  const isLarge = size === "large"

  return (
    <div className={className}>
      <label
        htmlFor={id}
        className={
          hideLabel
            ? "sr-only"
            : "mb-2 block text-[0.6875rem] font-medium uppercase tracking-[0.12em] text-muted-foreground"
        }
      >
        {label}
      </label>

      <div
        className={`flex items-center gap-2 rounded-md border bg-card transition-colors duration-150 ${
          isLarge ? "p-1.5" : "p-1"
        } ${isFocused ? "border-primary" : "border-border hover:border-foreground/25"}`}
      >
        <div className={`flex flex-1 items-center gap-2.5 ${isLarge ? "pl-3.5" : "pl-3"}`}>
          <MagnifyingGlass
            weight="bold"
            size={isLarge ? 17 : 15}
            className={`shrink-0 transition-colors duration-150 ${
              isFocused ? "text-primary" : "text-muted-foreground"
            }`}
          />
          <input
            id={id}
            type="search"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            placeholder={placeholder}
            className={`min-w-0 flex-1 border-0 bg-transparent text-foreground outline-none placeholder:text-muted-foreground [&::-webkit-search-cancel-button]:appearance-none ${
              isLarge ? "h-11 text-[0.9375rem]" : "h-9 text-sm"
            }`}
          />
          <AnimatePresence>
            {value && (
              <motion.button
                type="button"
                initial={reduce ? false : { opacity: 0, scale: 0.85 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={reduce ? undefined : { opacity: 0, scale: 0.85 }}
                transition={{ duration: 0.12 }}
                onClick={handleClear}
                aria-label="Clear search"
                className="btn-press shrink-0 rounded-sm p-1 text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
              >
                <X weight="bold" size={13} />
              </motion.button>
            )}
          </AnimatePresence>
        </div>

        <Button
          onClick={onSearch}
          size={isLarge ? "lg" : "default"}
          className={isLarge ? "shrink-0 px-7" : "shrink-0 px-5"}
        >
          Search
        </Button>
      </div>
    </div>
  )
}

export default SearchBar

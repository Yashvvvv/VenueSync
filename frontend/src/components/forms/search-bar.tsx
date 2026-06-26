"use client"

import type React from "react"

import { Search, X } from "lucide-react"
import { Input } from "../ui/input"
import { Button } from "../ui/button"
import { motion, AnimatePresence } from "framer-motion"
import { useState, useCallback } from "react"

interface SearchBarProps {
  value: string
  onChange: (value: string) => void
  onSearch: () => void
  placeholder?: string
  className?: string
  size?: "default" | "large"
}

export const SearchBar: React.FC<SearchBarProps> = ({
  value,
  onChange,
  onSearch,
  placeholder = "Search events, venues, or artists...",
  className = "",
  size = "default",
}) => {
  const [isFocused, setIsFocused] = useState(false)

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter") {
        onSearch()
      }
    },
    [onSearch],
  )

  const handleClear = () => {
    onChange("")
    onSearch()
  }

  const isLarge = size === "large"

  return (
    <div className={`relative ${className}`}>
      {/* Focus ring glow — very subtle */}
      {isFocused && (
        <div className="absolute -inset-px rounded-2xl bg-primary/10 blur-md pointer-events-none" />
      )}

      <div
        className={`relative flex items-center gap-2 rounded-2xl border transition-all duration-200 ${
          isLarge ? "p-2" : "p-1.5"
        } ${
          isFocused
            ? "border-primary/40 bg-card/80"
            : "border-border/50 bg-card/50 hover:border-border/70"
        }`}
        style={{ backdropFilter: "blur(20px)" }}
      >
        <div className={`flex-1 flex items-center gap-3 ${isLarge ? "px-4" : "px-3"}`}>
          <Search
            className={`flex-shrink-0 transition-colors ${
              isFocused ? "text-primary" : "text-muted-foreground"
            } ${isLarge ? "w-5 h-5" : "w-4 h-4"}`}
          />
          <Input
            type="text"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            placeholder={placeholder}
            className={`border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 text-foreground placeholder:text-muted-foreground/60 px-0 ${
              isLarge ? "text-base h-11" : "h-9 text-sm"
            }`}
          />
          <AnimatePresence>
            {value && (
              <motion.button
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                transition={{ duration: 0.15 }}
                onClick={handleClear}
                className="p-1 hover:bg-secondary/70 rounded-lg transition-colors flex-shrink-0"
              >
                <X className="w-3.5 h-3.5 text-muted-foreground" />
              </motion.button>
            )}
          </AnimatePresence>
        </div>

        <Button
          onClick={onSearch}
          className={`gradient-primary text-white rounded-xl shadow-md shadow-primary/20 font-medium flex-shrink-0 ${
            isLarge ? "px-7 h-11 text-sm" : "px-5 h-9 text-sm"
          }`}
        >
          Search
        </Button>
      </div>
    </div>
  )
}

export default SearchBar

"use client"

import type React from "react"

import { Search, X, Sparkles } from "lucide-react"
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
    <motion.div
      animate={{ scale: isFocused ? 1.01 : 1 }}
      transition={{ duration: 0.2 }}
      className={`relative ${className}`}
    >
      <AnimatePresence>
        {isFocused && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="absolute -inset-1 rounded-2xl bg-primary/20 blur-xl"
          />
        )}
      </AnimatePresence>

      <div
        className={`relative flex items-center gap-2 glass-strong rounded-2xl transition-all duration-300 ${
          isLarge ? "p-2" : "p-1.5"
        } ${isFocused ? "ring-2 ring-primary/40 border-primary/40" : "border-border/40"}`}
      >
        <div className={`flex-1 flex items-center gap-3 ${isLarge ? "px-4" : "px-3"}`}>
          <Search className={`text-muted-foreground flex-shrink-0 ${isLarge ? "w-6 h-6" : "w-5 h-5"}`} />
          <Input
            type="text"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            placeholder={placeholder}
            className={`border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 text-foreground placeholder:text-muted-foreground px-0 ${
              isLarge ? "text-lg h-12" : "h-10"
            }`}
          />
          <AnimatePresence>
            {value && (
              <motion.button
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                onClick={handleClear}
                className="p-1.5 hover:bg-secondary rounded-lg transition-colors"
              >
                <X className="w-4 h-4 text-muted-foreground" />
              </motion.button>
            )}
          </AnimatePresence>
        </div>

        <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
          <Button
            onClick={onSearch}
            className={`gradient-primary text-white rounded-xl shadow-lg shadow-primary/20 ${
              isLarge ? "px-8 h-12 text-base" : "px-6"
            }`}
          >
            <Sparkles className={`mr-2 ${isLarge ? "w-5 h-5" : "w-4 h-4"}`} />
            Search
          </Button>
        </motion.div>
      </div>
    </motion.div>
  )
}

export default SearchBar

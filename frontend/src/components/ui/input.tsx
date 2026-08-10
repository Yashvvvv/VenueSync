import * as React from "react";

import { cn } from "@/lib/utils";

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "flex h-9 w-full min-w-0 rounded-md border border-border bg-card px-3 py-1 text-base text-foreground outline-none transition-colors duration-150 file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground selection:bg-primary/30 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-45 md:text-sm",
        // Border change is the focus signal. A blurred ring on a dark
        // surface reads as a glow, which is the look we are moving away from.
        "hover:border-foreground/25 focus-visible:border-primary",
        "aria-invalid:border-destructive",
        className,
      )}
      {...props}
    />
  );
}

export { Input };

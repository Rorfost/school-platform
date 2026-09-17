import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Input, type InputProps } from "@/components/ui/Input";

export function PasswordInput(props: Omit<InputProps, "type" | "rightElement">) {
  const [isVisible, setIsVisible] = useState(false);
  const label = isVisible ? "Hide password" : "Show password";

  return (
    <Input
      {...props}
      type={isVisible ? "text" : "password"}
      rightElement={
        <button
          type="button"
          aria-label={label}
          aria-pressed={isVisible}
          onClick={() => setIsVisible((visible) => !visible)}
          className="flex size-11 items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-900"
        >
          {isVisible ? (
            <EyeOff size={17} aria-hidden="true" />
          ) : (
            <Eye size={17} aria-hidden="true" />
          )}
        </button>
      }
    />
  );
}

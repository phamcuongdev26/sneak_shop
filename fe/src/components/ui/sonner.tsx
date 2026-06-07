"use client"

import { Toaster as SonnerToaster } from "sonner"

type ToasterProps = React.ComponentProps<typeof SonnerToaster>

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <SonnerToaster
      position="bottom-right"
      richColors
      closeButton
      duration={7000}
      {...props}
    />
  )
}

export { Toaster }

import { useState } from "react";
import { Copy, Check, QrCode } from "lucide-react";
import { toast } from "sonner";
import { buildPixPayload, qrImageUrl, formatBRLCents } from "@/lib/pix";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

export function PixDialog({
  amountCents,
  pixKey,
  merchant,
  description,
  trigger,
  onConfirm,
}: {
  amountCents: number;
  pixKey: string | null | undefined;
  merchant: string;
  description: string;
  trigger: React.ReactNode;
  onConfirm?: () => void | Promise<void>;
}) {
  const [copied, setCopied] = useState(false);
  const [open, setOpen] = useState(false);

  const payload = pixKey
    ? buildPixPayload({
        key: pixKey,
        amount: amountCents / 100,
        merchant,
        description,
      })
    : "";

  async function copy() {
    await navigator.clipboard.writeText(payload);
    setCopied(true);
    toast.success("Código Pix copiado");
    setTimeout(() => setCopied(false), 2500);
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent className="max-w-sm rounded-3xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <QrCode className="size-5 text-primary" /> Pagar com Pix
          </DialogTitle>
          <DialogDescription>
            {description} · <span className="font-bold text-primary">{formatBRLCents(amountCents)}</span>
          </DialogDescription>
        </DialogHeader>

        {!pixKey ? (
          <p className="text-sm text-muted-foreground">
            O organizador ainda não cadastrou uma chave Pix. Combine o pagamento direto com ele.
          </p>
        ) : (
          <div className="space-y-3">
            <img
              src={qrImageUrl(payload)}
              alt="QR Code Pix para pagamento"
              className="mx-auto size-52 rounded-2xl bg-white p-2"
              loading="lazy"
            />
            <p className="break-all rounded-2xl bg-secondary p-3 text-[0.65rem] leading-relaxed text-muted-foreground">
              {payload}
            </p>
            <Button onClick={copy} className="w-full" variant="secondary">
              {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
              {copied ? "Copiado!" : "Copiar código Pix"}
            </Button>
            {onConfirm && (
              <Button
                className="w-full"
                onClick={async () => {
                  await onConfirm();
                  setOpen(false);
                }}
              >
                Já paguei
              </Button>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

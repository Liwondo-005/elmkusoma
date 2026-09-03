import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { CertificateVerifier } from "@/components/certificates/certificate-verifier"

export default function VerifyCertificatePage() {
  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="border-b border-border bg-muted/40">
          <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
            <span className="inline-flex items-center gap-1.5 rounded-md bg-orange/10 px-2.5 py-1 text-xs font-semibold text-orange">
              Certificate Verification
            </span>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
              Verify a Certificate
            </h1>
            <p className="mt-2 max-w-2xl text-muted-foreground">
              Enter a certificate ID below to confirm its authenticity and view the full certificate details.
            </p>
          </div>
        </section>
        <CertificateVerifier />
      </main>
      <SiteFooter />
    </div>
  )
}

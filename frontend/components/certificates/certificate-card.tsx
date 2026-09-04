import type { Certificate } from "@/lib/data"

export function CertificateCard({ certificate }: { certificate: Certificate }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-xs">
      <div className="border-b border-border bg-accent/50 px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-xl text-orange bg-orange/10">
            ★
          </div>
          <div>
            <p className="text-xs font-medium text-muted-foreground">Certificate of Completion</p>
            <p className="text-sm font-semibold text-foreground">ELMKUSOMA</p>
          </div>
        </div>
      </div>

      <div className="px-6 py-6">
        <p className="text-center text-xs font-medium uppercase tracking-widest text-muted-foreground">
          This is to certify that
        </p>
        <p className="mt-2 text-center text-2xl font-bold text-foreground">{certificate.studentName}</p>
        <p className="mt-2 text-center text-xs text-muted-foreground">
          has successfully completed the course
        </p>
        <p className="mt-2 text-center text-lg font-semibold text-primary">{certificate.courseTitle}</p>

        <div className="mt-6 space-y-3 rounded-xl border border-border bg-muted/40 p-4">
          <div className="flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">Instructor:</span>
            <span className="font-medium text-foreground">{certificate.instructor}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">Completed:</span>
            <span className="font-medium text-foreground">{certificate.completionDate}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">Grade:</span>
            <span className="font-medium text-foreground">{certificate.grade}</span>
          </div>
        </div>

        {certificate.skills.length > 0 && (
          <div className="mt-5">
            <p className="text-xs font-medium text-muted-foreground">Skills Acquired</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {certificate.skills.map((skill) => (
                <span
                  key={skill}
                  className="inline-flex items-center rounded-md bg-accent px-2.5 py-1 text-xs font-medium text-accent-foreground"
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="mt-6 flex items-center justify-center gap-2 rounded-xl border border-teal/20 bg-teal/5 px-4 py-3">
          <span className="text-sm font-semibold text-teal">✓ Verified Certificate</span>
        </div>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          Certificate ID: <span className="font-mono font-medium text-foreground">{certificate.id}</span>
        </p>
      </div>
    </div>
  )
}

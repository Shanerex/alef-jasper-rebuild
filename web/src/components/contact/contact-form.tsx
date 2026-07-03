"use client";

import { useState, useRef } from "react";
import { submitLead } from "@/lib/api/leads";
import type { LeadPayload } from "@/lib/types/lead";

/**
 * Contact form — client component (design §7, F11-AC6).
 *
 * Posts to POST /api/leads via submitLead(). Four state machine states:
 *   idle       → form shown, button "Send Enquiry"
 *   submitting → all fields + button disabled, button "Sending…"
 *   success    → form replaced by confirmation panel (201 response)
 *   error      → form shown with top-of-form banner; errorKind determines copy
 *                 validation | rate_limit | server
 *
 * The website field is a honeypot: hidden from humans, out of tab order,
 * aria-hidden. A real user leaves it blank. The server returns 201 for bot
 * submissions without persisting a row — the bot cannot detect the trap.
 *
 * Client validation runs before POST for instant feedback but the server
 * re-validates authoritatively. Gold left-border on field errors (no new color).
 *
 * Accessibility: honeypot aria-hidden + tabIndex=-1; error messages linked
 * via aria-describedby; disabled uses the HTML disabled attribute; visible
 * gold focus outline on inputs.
 */

type Status = "idle" | "submitting" | "success" | "error";
type ErrorKind = "validation" | "rate_limit" | "server";

interface FieldErrors {
  name?: string;
  email?: string;
  message?: string;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function ContactForm() {
  const [status, setStatus] = useState<Status>("idle");
  const [errorKind, setErrorKind] = useState<ErrorKind>("server");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  // Form field refs for reading values on submit
  const nameRef = useRef<HTMLInputElement>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  const phoneRef = useRef<HTMLInputElement>(null);
  const companyRef = useRef<HTMLInputElement>(null);
  const messageRef = useRef<HTMLTextAreaElement>(null);
  const websiteRef = useRef<HTMLInputElement>(null); // honeypot

  function resetToIdle() {
    setStatus("idle");
    setFieldErrors({});
    setErrorKind("server");
    // Clear fields on reset (for "Send another" flow)
    if (nameRef.current) nameRef.current.value = "";
    if (emailRef.current) emailRef.current.value = "";
    if (phoneRef.current) phoneRef.current.value = "";
    if (companyRef.current) companyRef.current.value = "";
    if (messageRef.current) messageRef.current.value = "";
    if (websiteRef.current) websiteRef.current.value = "";
  }

  /** Client-side validation — mirrors server rules for instant feedback. */
  function validate(): FieldErrors | null {
    const errors: FieldErrors = {};
    const name = nameRef.current?.value.trim() ?? "";
    const email = emailRef.current?.value.trim() ?? "";
    const message = messageRef.current?.value.trim() ?? "";

    if (!name) errors.name = "Please enter your name.";
    if (!email || !EMAIL_RE.test(email))
      errors.email = "Please enter a valid email address.";
    if (!message) errors.message = "Please tell us how we can help.";

    return Object.keys(errors).length > 0 ? errors : null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    // Client validation
    const errors = validate();
    if (errors) {
      setFieldErrors(errors);
      setErrorKind("validation");
      setStatus("error");
      return;
    }

    setStatus("submitting");
    setFieldErrors({});

    const payload: LeadPayload = {
      name: nameRef.current?.value.trim() ?? "",
      email: emailRef.current?.value.trim() ?? "",
      phone: phoneRef.current?.value.trim() ?? "",
      company: companyRef.current?.value.trim() ?? "",
      message: messageRef.current?.value.trim() ?? "",
      website: websiteRef.current?.value ?? "", // honeypot
    };

    try {
      await submitLead(payload);
      setStatus("success");
    } catch (err: unknown) {
      const httpStatus = (err as { status?: number }).status;
      if (httpStatus === 400) {
        setErrorKind("validation");
      } else if (httpStatus === 429) {
        setErrorKind("rate_limit");
      } else {
        setErrorKind("server");
      }
      setStatus("error");
    }
  }

  // ── Success panel ────────────────────────────────────────────────────────────
  if (status === "success") {
    return (
      <div className="max-w-[560px]">
        <div className="mb-5 h-[1px] w-7 bg-gold" />
        <p
          className="mb-3 font-sans text-[10px] font-bold uppercase text-gold"
          style={{ letterSpacing: "0.22em" }}
        >
          Enquiry received
        </p>
        <h3 className="mb-4 font-serif text-[26px] font-normal text-text-primary">
          Thank you — we&rsquo;ve got your enquiry.
        </h3>
        <p className="mb-8 font-sans text-[13px] font-normal leading-[1.7] text-text-muted">
          A member of the ALEF team will get back to you shortly. For anything
          urgent, call the Dubai office on{" "}
          <a href="tel:+97142513840" className="text-gold hover:text-gold-light">
            +971 4 2513840
          </a>
          .
        </p>
        <button
          onClick={resetToIdle}
          className="font-sans text-[10px] font-bold uppercase text-gold transition-colors hover:text-gold-light"
          style={{ letterSpacing: "0.12em" }}
        >
          Send another enquiry &rarr;
        </button>
      </div>
    );
  }

  // ── Top-of-form error banner copy ────────────────────────────────────────────
  const errorBanner: Record<ErrorKind, string> = {
    validation:
      "Please check the highlighted fields and try again.",
    rate_limit:
      "You’ve sent a few enquiries already. Please wait a little while before sending another — or call us on +971 4 2513840.",
    server:
      "Something went wrong on our side and your enquiry didn’t send. Please try again, or email us directly at alefllc@eim.ae.",
  };

  const isSubmitting = status === "submitting";
  const hasError = status === "error";

  // ── Form ─────────────────────────────────────────────────────────────────────
  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className="max-w-[560px] space-y-6"
    >
      {/* Top-of-form error banner */}
      {hasError && (
        <div
          className="border-l-2 border-gold py-2 pl-4 font-sans text-[12px] font-normal text-text-secondary"
          role="alert"
        >
          {errorBanner[errorKind]}
        </div>
      )}

      {/* Name */}
      <Field
        id="cf-name"
        label="Your Name"
        error={fieldErrors.name}
        required
      >
        <input
          ref={nameRef}
          id="cf-name"
          type="text"
          placeholder="Full name"
          autoComplete="name"
          disabled={isSubmitting}
          aria-required="true"
          aria-describedby={fieldErrors.name ? "cf-name-err" : undefined}
          className="w-full bg-surface-2 px-4 py-3 font-sans text-[12px] font-light text-text-primary placeholder-text-disabled outline-none transition-colors focus:border-gold disabled:opacity-50"
          style={{
            border: fieldErrors.name
              ? "1px solid rgba(196,151,58,0.25)"
              : "1px solid rgba(196,151,58,0.25)",
            borderLeft: fieldErrors.name ? "2px solid #C4973A" : undefined,
          }}
        />
        {fieldErrors.name && (
          <p id="cf-name-err" className="mt-1 font-sans text-[11px] text-text-secondary">
            {fieldErrors.name}
          </p>
        )}
      </Field>

      {/* Email */}
      <Field
        id="cf-email"
        label="Email"
        error={fieldErrors.email}
        required
      >
        <input
          ref={emailRef}
          id="cf-email"
          type="email"
          placeholder="you@company.com"
          autoComplete="email"
          disabled={isSubmitting}
          aria-required="true"
          aria-describedby={fieldErrors.email ? "cf-email-err" : undefined}
          className="w-full bg-surface-2 px-4 py-3 font-sans text-[12px] font-light text-text-primary placeholder-text-disabled outline-none transition-colors focus:border-gold disabled:opacity-50"
          style={{
            border: "1px solid rgba(196,151,58,0.25)",
            borderLeft: fieldErrors.email ? "2px solid #C4973A" : undefined,
          }}
        />
        {fieldErrors.email && (
          <p id="cf-email-err" className="mt-1 font-sans text-[11px] text-text-secondary">
            {fieldErrors.email}
          </p>
        )}
      </Field>

      {/* Phone (optional) */}
      <Field id="cf-phone" label="Phone (optional)">
        <input
          ref={phoneRef}
          id="cf-phone"
          type="tel"
          placeholder="+971 …"
          autoComplete="tel"
          disabled={isSubmitting}
          className="w-full bg-surface-2 px-4 py-3 font-sans text-[12px] font-light text-text-primary placeholder-text-disabled outline-none transition-colors focus:border-gold disabled:opacity-50"
          style={{ border: "1px solid rgba(196,151,58,0.25)" }}
        />
      </Field>

      {/* Company (optional) */}
      <Field id="cf-company" label="Company (optional)">
        <input
          ref={companyRef}
          id="cf-company"
          type="text"
          placeholder="Company name"
          autoComplete="organization"
          disabled={isSubmitting}
          className="w-full bg-surface-2 px-4 py-3 font-sans text-[12px] font-light text-text-primary placeholder-text-disabled outline-none transition-colors focus:border-gold disabled:opacity-50"
          style={{ border: "1px solid rgba(196,151,58,0.25)" }}
        />
      </Field>

      {/* Message */}
      <Field
        id="cf-message"
        label="How can we help?"
        error={fieldErrors.message}
        required
      >
        <textarea
          ref={messageRef}
          id="cf-message"
          rows={5}
          placeholder="Tell us about your project, scope, or timeline."
          disabled={isSubmitting}
          aria-required="true"
          aria-describedby={fieldErrors.message ? "cf-message-err" : undefined}
          className="w-full resize-y bg-surface-2 px-4 py-3 font-sans text-[12px] font-light text-text-primary placeholder-text-disabled outline-none transition-colors focus:border-gold disabled:opacity-50"
          style={{
            border: "1px solid rgba(196,151,58,0.25)",
            borderLeft: fieldErrors.message ? "2px solid #C4973A" : undefined,
          }}
        />
        {fieldErrors.message && (
          <p id="cf-message-err" className="mt-1 font-sans text-[11px] text-text-secondary">
            {fieldErrors.message}
          </p>
        )}
      </Field>

      {/* Honeypot — hidden from real users, must not interfere with normal form flow */}
      <div style={{ position: "absolute", left: "-9999px" }} aria-hidden="true">
        <label htmlFor="cf-website">Website</label>
        <input
          ref={websiteRef}
          id="cf-website"
          name="website"
          type="text"
          tabIndex={-1}
          autoComplete="off"
        />
      </div>

      {/* Submit button */}
      {isSubmitting ? (
        <button
          type="button"
          disabled
          className="w-full bg-page px-8 py-[14px] font-sans text-[10px] font-bold uppercase text-text-muted opacity-50 sm:w-auto"
          style={{
            letterSpacing: "0.14em",
            border: "1px solid rgba(196,151,58,0.25)",
          }}
        >
          Sending&hellip;
        </button>
      ) : (
        <button
          type="submit"
          className="w-full bg-gold px-8 py-[14px] font-sans text-[10px] font-bold uppercase text-page transition-colors hover:bg-gold-light sm:w-auto"
          style={{ letterSpacing: "0.14em" }}
        >
          Send Enquiry
        </button>
      )}
    </form>
  );
}

/** Shared field wrapper: label + slot + optional error message. */
function Field({
  id,
  label,
  error,
  required,
  children,
}: {
  id: string;
  label: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label
        htmlFor={id}
        className="mb-2 block font-sans text-[8.5px] font-bold uppercase text-gold"
        style={{ letterSpacing: "0.2em" }}
      >
        {label}
        {required && <span className="sr-only"> (required)</span>}
      </label>
      {children}
    </div>
  );
}

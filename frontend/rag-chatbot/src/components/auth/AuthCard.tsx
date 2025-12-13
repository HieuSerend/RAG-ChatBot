import React from "react";

type Props = {
  title: string;
  children: React.ReactNode;
};

export default function AuthCard({ title, children }: Props) {
  return (
    <div className="w-[400px] p-8 rounded-2xl backdrop-blur-md bg-slate-900/60 border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.4)] text-center relative overflow-hidden">
      {/* Subtle top light effect */}
      <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-blue-500/50 to-transparent opacity-50" />

      <h2 className="m-0 mb-6 text-2xl text-white font-bold tracking-tight">{title}</h2>
      <div className="mt-1">{children}</div>
    </div>
  );
}

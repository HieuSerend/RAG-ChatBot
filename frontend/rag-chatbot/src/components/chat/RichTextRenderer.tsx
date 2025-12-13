import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkMath from 'remark-math';
import remarkGfm from 'remark-gfm';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

// --- Utility for Tailwind ---
function cn(...inputs: (string | undefined | null | false)[]) {
    return twMerge(clsx(inputs));
}

interface RichTextRendererProps {
    content: string;
    className?: string;
    role?: 'user' | 'ai';
}

const RichTextRenderer: React.FC<RichTextRendererProps> = ({ content, className }) => {
    return (
        <div className={cn("markdown-body font-sans text-sm leading-relaxed", className)}>
            <ReactMarkdown
                remarkPlugins={[remarkMath, remarkGfm]}
                rehypePlugins={[rehypeKatex]}
                components={{
                    // Paragraphs
                    p: ({ children }) => <p className="mb-2 last:mb-0 text-inherit leading-7">{children}</p>,

                    // Headings
                    h1: ({ children }) => <h1 className="text-xl font-bold mb-3 mt-4 text-emerald-400 first:mt-0">{children}</h1>,
                    h2: ({ children }) => <h2 className="text-lg font-semibold mb-2 mt-3 text-slate-200">{children}</h2>,
                    h3: ({ children }) => <h3 className="text-base font-medium mb-1 mt-2 text-slate-300">{children}</h3>,

                    // Lists
                    ul: ({ children }) => <ul className="list-disc pl-5 mb-2 space-y-1 text-inherit marker:text-emerald-500/50">{children}</ul>,
                    ol: ({ children }) => <ol className="list-decimal pl-5 mb-2 space-y-1 text-inherit marker:text-emerald-500/50">{children}</ol>,
                    li: ({ children }) => <li className="pl-1 text-inherit">{children}</li>,

                    // Code
                    code: ({ node, className, children, ...props }) => {
                        const match = /language-(\w+)/.exec(className || '');
                        const isInline = !match;
                        return isInline ? (
                            <code className="bg-slate-800/50 text-emerald-300 px-1.5 py-0.5 rounded text-[0.9em] font-mono border border-slate-700/50" {...props}>
                                {children}
                            </code>
                        ) : (
                            <div className="my-3 rounded-lg overflow-hidden border border-slate-700 bg-slate-950">
                                <div className="bg-slate-900/50 px-3 py-1.5 border-b border-slate-800 text-xs text-slate-500 font-mono flex justify-between">
                                    <span>{match?.[1] || 'code'}</span>
                                </div>
                                <div className="p-3 overflow-x-auto">
                                    <pre className={className}>
                                        <code className="font-mono text-sm text-slate-300" {...props}>
                                            {children}
                                        </code>
                                    </pre>
                                </div>
                            </div>
                        );
                    },

                    // Quotes
                    blockquote: ({ children }) => (
                        <blockquote className="border-l-2 border-emerald-500/50 pl-4 py-1 my-2 bg-slate-800/20 text-slate-400 italic rounded-r-sm">
                            {children}
                        </blockquote>
                    ),

                    // Links
                    a: ({ href, children }) => (
                        <a
                            href={href}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-emerald-400 hover:text-emerald-300 underline underline-offset-2 transition-colors"
                        >
                            {children}
                        </a>
                    ),

                    // Tables
                    table: ({ children }) => (
                        <div className="overflow-x-auto my-4 rounded-lg border border-slate-700">
                            <table className="w-full text-left text-sm text-slate-300">
                                {children}
                            </table>
                        </div>
                    ),
                    thead: ({ children }) => <thead className="bg-slate-900 text-slate-200 uppercase text-xs font-semibold">{children}</thead>,
                    tbody: ({ children }) => <tbody className="divide-y divide-slate-800 bg-slate-900/40">{children}</tbody>,
                    tr: ({ children }) => <tr className="hover:bg-slate-800/50 transition-colors">{children}</tr>,
                    th: ({ children }) => <th className="px-4 py-3 whitespace-nowrap">{children}</th>,
                    td: ({ children }) => <td className="px-4 py-3 whitespace-nowrap">{children}</td>,

                    // Text Formatting
                    strong: ({ children }) => <strong className="font-bold text-slate-100">{children}</strong>,
                    em: ({ children }) => <em className="italic text-slate-300">{children}</em>,
                    hr: () => <hr className="my-4 border-slate-700" />,
                }}
            >
                {content}
            </ReactMarkdown>
        </div>
    );
};

export default RichTextRenderer;

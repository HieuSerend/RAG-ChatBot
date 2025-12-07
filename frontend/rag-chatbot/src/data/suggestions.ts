import {
    Sigma,
    TrendingUp,
    Scale,
    BookOpen,
    Activity,
    CandlestickChart,
    Percent,
    ShieldAlert,
    BarChart4,
    ArrowRightLeft,
    Coins,
    Wallet,
    Landmark,
    BrainCircuit,
    LineChart,
    type LucideIcon
} from 'lucide-react';

export interface SuggestionItem {
    id: string;
    title: string;
    description: string;
    iconName: string;
    fullPrompt: string;
}

export const SUGGESTIONS_POOL: SuggestionItem[] = [
    // --- Quantitative Finance ---
    {
        id: 'q1',
        title: 'Explain Gamma',
        description: 'Sensitivity of Delta to price changes.',
        iconName: 'Sigma',
        fullPrompt: 'Explain the concept of Gamma in options trading, its relationship with Delta and Theta, and how it affects portfolio risk.'
    },
    {
        id: 'q2',
        title: 'Black-Scholes Model',
        description: 'Assumptions and pricing formula derivation.',
        iconName: 'BookOpen',
        fullPrompt: 'Derive the Black-Scholes partial differential equation and discuss the key assumptions behind the model, such as constant volatility and risk-free rate.'
    },
    {
        id: 'q3',
        title: 'Stochastic Calculus',
        description: 'Itô’s Lemma application in finance.',
        iconName: 'Activity',
        fullPrompt: 'Explain Itô’s Lemma and its significance in stochastic calculus for modeling asset prices, specifically within the geometric Brownian motion framework.'
    },
    {
        id: 'q4',
        title: 'Monte Carlo Simulation',
        description: 'Path-dependent option pricing method.',
        iconName: 'LineChart',
        fullPrompt: 'Describe how Monte Carlo simulations are used to price path-dependent options (like Asian or Barrier options) where analytical solutions are difficult to derive.'
    },

    // --- DeFi Mechanisms ---
    {
        id: 'd1',
        title: 'AMM Mechanics',
        description: 'Uniswap constant product formula.',
        iconName: 'ArrowRightLeft',
        fullPrompt: 'Explain the Constant Product Market Maker (x * y = k) model used by Uniswap v2 and how it determines price discovery and slippage.'
    },
    {
        id: 'd2',
        title: 'Impermanent Loss',
        description: 'Risks of liquidity provision.',
        iconName: 'ShieldAlert',
        fullPrompt: 'Analyze the concept of Impermanent Loss in Automated Market Makers (AMMs). Under what volatility conditions does providing liquidity become unprofitable compared to HODLing?'
    },
    {
        id: 'd3',
        title: 'Staking vs. Farming',
        description: 'Comparing yield generation strategies.',
        iconName: 'Coins',
        fullPrompt: 'Compare and contrast Staking (PoS consensus) vs. Yield Farming (Liquidity Mining) in DeFi terms of risk profile, source of yield, and smart contract dependencies.'
    },
    {
        id: 'd4',
        title: 'Flash Loans',
        description: 'Uncollateralized arbitrage execution.',
        iconName: 'BrainCircuit',
        fullPrompt: 'Explain the mechanics of Flash Loans in DeFi. How do atomic transactions ensure loan safety, and what are their primary use cases in arbitrage and liquidation?'
    },

    // --- Risk Management ---
    {
        id: 'r1',
        title: 'Value at Risk (VaR)',
        description: 'Quantifying potential portfolio losses.',
        iconName: 'BarChart4',
        fullPrompt: 'Define Value at Risk (VaR) and compare the Historical, Variance-Covariance, and Monte Carlo methods for calculating it. What are the limitations of VaR during tail events?'
    },
    {
        id: 'r2',
        title: 'Sharpe Ratio',
        description: 'Risk-adjusted return performance metric.',
        iconName: 'Percent',
        fullPrompt: 'How is the Sharpe Ratio calculated? Discuss its weaknesses when applied to return distributions with high skewness or kurtosis (non-normal distributions).'
    },
    {
        id: 'r3',
        title: 'Maximum Drawdown',
        description: 'Peak-to-trough decline measurement.',
        iconName: 'TrendingUp', // Using TrendingUp inversely or generally for performance
        fullPrompt: 'Explain Maximum Drawdown (MDD) as a risk metric. Why is it often considered more critical than standard deviation for evaluating hedge fund performance?'
    },
    {
        id: 'r4',
        title: 'Hedging with Greeks',
        description: 'Delta-neutral portfolio management.',
        iconName: 'Scale',
        fullPrompt: 'Describe the process of creating a Delta-Neutral portfolio. How does dynamic hedging help manage Gamma risk, and what are the transaction cost implications?'
    },

    // --- Trading Concepts ---
    {
        id: 't1',
        title: 'Mean Reversion',
        description: 'Statistical arbitrage strategy basics.',
        iconName: 'CandlestickChart',
        fullPrompt: 'Explain the theory of Mean Reversion in asset pricing. How do Ornstein-Uhlenbeck processes model this behavior mathematically?'
    },
    {
        id: 't2',
        title: 'Momentum Strategy',
        description: 'Trend-following implementation.',
        iconName: 'Activity',
        fullPrompt: 'What is the theoretical basis for Momentum strategies (Time-Series vs. Cross-Sectional)? Discuss the behavioral finance explanations for why momentum persists.'
    },
    {
        id: 't3',
        title: 'Market Making',
        description: 'Bid-ask spread liquidity provision.',
        iconName: 'Wallet',
        fullPrompt: 'Explain the role of a Market Maker in an order book exchange. How do they manage inventory risk and what is the concept of "Adverse Selection"?'
    }
];

// --- Utilities ---

export const iconMap: Record<string, LucideIcon> = {
    Sigma,
    BookOpen,
    Activity,
    LineChart,
    ArrowRightLeft,
    ShieldAlert,
    Coins,
    BrainCircuit,
    BarChart4,
    Percent,
    TrendingUp,
    Scale,
    CandlestickChart,
    Wallet,
    Landmark
};

/**
 * Returns a random subset of suggestions from the pool.
 * @param count Number of suggestions to return (default 4)
 */
export function getRandomSuggestions(count: number = 4): SuggestionItem[] {
    const shuffled = [...SUGGESTIONS_POOL].sort(() => 0.5 - Math.random());
    return shuffled.slice(0, count);
}

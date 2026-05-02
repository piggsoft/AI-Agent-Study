/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    darkMode: 'class',
    theme: {
        extend: {
            animation: {
                'float': 'float 3s ease-in-out infinite',
                'slide-in': 'slideIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards',
                'shimmer': 'shimmer 1.5s infinite',
            },
            keyframes: {
                float: {
                    '0%, 100%': {transform: 'translateY(0)'},
                    '50%': {transform: 'translateY(-8px)'},
                },
                slideIn: {
                    from: {opacity: '0', transform: 'translateY(20px) scale(0.98)'},
                    to: {opacity: '1', transform: 'translateY(0) scale(1)'},
                },
                shimmer: {
                    '0%': {backgroundPosition: '-200% 0'},
                    '100%': {backgroundPosition: '200% 0'},
                },
            },
        },
    },
    plugins: [
        require('@tailwindcss/typography'),
    ],
}

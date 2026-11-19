/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ember: {
          50: '#fff5ed',
          100: '#ffe8d3',
          200: '#ffcda6',
          400: '#f68428',
          500: '#e8620c',
          600: '#c44a08',
          700: '#9a380b',
          900: '#5a210c',
        },
        charcoal: '#241812',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
      },
    },
  },
  plugins: [],
}

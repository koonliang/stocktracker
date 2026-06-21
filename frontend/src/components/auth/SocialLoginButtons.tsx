type SocialProvider = 'google' | 'facebook';

type Props = {
  onClick: (provider: SocialProvider) => void;
  disabled?: boolean;
};

function GoogleMark() {
  return (
    <svg aria-hidden viewBox="0 0 18 18" className="h-5 w-5">
      <path
        fill="#4285F4"
        d="M17.64 9.2045c0-.6382-.0573-1.2518-.1636-1.8409H9v3.4818h4.8436c-.2086 1.125-.8427 2.0782-1.7959 2.7164v2.2582h2.9086c1.7018-1.5668 2.6837-3.875 2.6837-6.6155Z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.4673-.8059 5.9564-2.1791l-2.9086-2.2582c-.8059.54-1.8368.8591-3.0477.8591-2.3477 0-4.335-1.5859-5.0441-3.7168H.9491v2.3327A8.9995 8.9995 0 0 0 9 18Z"
      />
      <path
        fill="#FBBC05"
        d="M3.9559 10.7041A5.4095 5.4095 0 0 1 3.6736 9c0-.5905.1023-1.1645.2823-1.7041V4.9632H.9491A8.9994 8.9994 0 0 0 0 9c0 1.4518.3477 2.8277.9491 4.0368l3.0068-2.3327Z"
      />
      <path
        fill="#EA4335"
        d="M9 3.5782c1.3214 0 2.5077.4541 3.4405 1.3459l2.5813-2.5814C13.4632.8918 11.426 0 9 0A8.9995 8.9995 0 0 0 .9491 4.9632l3.0068 2.3327C4.665 5.1641 6.6523 3.5782 9 3.5782Z"
      />
    </svg>
  );
}

function FacebookMark() {
  return (
    <svg aria-hidden viewBox="0 0 18 18" className="h-5 w-5">
      <path
        fill="currentColor"
        d="M18 9.054C18 4.0545 13.9705 0 9 0S0 4.0545 0 9.054c0 4.518 3.2918 8.2637 7.5938 8.9455v-6.3282H5.308V9.054h2.2858V7.059c0-2.268 1.3432-3.5212 3.4005-3.5212.9844 0 2.013.1777 2.013.1777v2.2275h-1.134c-1.116 0-1.4648.696-1.4648 1.4107V9.054h2.492L12.5025 11.6713H10.4087V18C14.7105 17.3182 18 13.5725 18 9.054Z"
      />
    </svg>
  );
}

const buttonClass =
  'flex h-11 items-center justify-center gap-2.5 rounded-md border border-border bg-surface ' +
  'text-small font-medium text-text transition-colors duration-[120ms] ' +
  'hover:border-border-strong hover:bg-surface-alt ' +
  'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring ' +
  'disabled:cursor-not-allowed disabled:opacity-60';

export function SocialLoginButtons({ onClick, disabled = false }: Props) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <button
        type="button"
        data-testid="social-login-google"
        aria-label="Continue with Google"
        className={buttonClass}
        onClick={() => onClick('google')}
        disabled={disabled}
      >
        <span data-testid="social-icon-google" className="flex items-center">
          <GoogleMark />
        </span>
        Google
      </button>
      <button
        type="button"
        data-testid="social-login-facebook"
        aria-label="Continue with Facebook"
        className={buttonClass}
        onClick={() => onClick('facebook')}
        disabled={disabled}
      >
        <span data-testid="social-icon-facebook" className="flex items-center text-[#1877f2]">
          <FacebookMark />
        </span>
        Facebook
      </button>
    </div>
  );
}

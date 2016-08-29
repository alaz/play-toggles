## Feature Toggles, aka. Features, Toggles, Flags, etc.

* [Martin Fowler: Feature Toggles](http://martinfowler.com/articles/feature-toggles.html)
* [Martin Fowler: FeatureToggle](http://martinfowler.com/bliki/FeatureToggle.html)
* [Java: Feature Flags, Toggles, Controls](http://featureflags.io/java-feature-flags/)

## Why another library?

* Very short & simple
* Compatible with its PHP counterpart: [php-toggles](https://github.com/osinka/php-toggles)

## Features

* Calculates strategies only once per browser session and then caches the result in a session cookie
* `Gradual` rollout strategy does not depend on user ID per se. Instead, it may be given any user-related textual ID.
* `Whitelist` and `Blacklist` depend on the same user ID.
* Provides `AnonymousOn` (enables a feature for anonymous users) and `AnonymousOff` (disables a feature for anonymous users)

## Use

Init:

```scala
import com.osinka.play.toggles._

object Toggles extends ToggleRegistry {
  private val authReqPf: PartialFunction[RequestHeader,String] = {
    case ar: AuthenticatedRequest[_, _] => ar.user.toString
  }

  private object UserStrategies extends UserStrategies(authReqPf.lift)

  val socialBlock = toggle("socialBlock")(
    Strategies.internalNet,
    UserStrategies.anonymousOff,
    UserStrategies.whitelist
  )
  val testedAndOn = toggle("testedAndOn")(
    Strategies.internalNet,
    UserStrategies.anonymousOff,
  )
  val inDevelopment = toggle("inDevelopment")(
    Strategies.internalNet,
    UserStrategies.anonymousOff,
  )
}
```

You would need to bind `ToggleRegistry` to `Toggles` implementation:

```
class Module extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[ToggleRegistry]).to(Toggles)
  }
}
```

In `conf/application.conf`:

```
toggles {
  socialBlock {
    whitelist = ["123456", "98765"]
  }
  testedAndOn.enabled = true
  inDevelopment.enabled = false
}
```

Strategies will be calculated only if `enabled` is not defined in the configuration. The order of checks is as follows:

1. if the feature is enabled in the configuration, it is active
2. if the feature is in a session cookie, it is active
3. if the feature is disabled in the configuration, it is active
4. then the strategies get checked one by one. The first that gives a definite result wins.

In order to check the toggles, either a filter should be used:

```scala
object Filter @Inject() (toggles: ToggleFilter) extends DefaultHttpFilters(toggles)
```

or wrap specific actions into `ToggleAction(Toggles)`

Then check the toggle anywhere (depends on `implicit requestHeader`):

```scala
if (Toggles.socialBlock.active) {
}
```

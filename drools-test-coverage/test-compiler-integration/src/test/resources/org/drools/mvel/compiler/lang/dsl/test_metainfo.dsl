[keyword][*]regra {atributos} faça {rhs} se {lhs} fim=rule {atributos} \\n when\\n {lhs}\\n then\\n {rhs}\\n end
[keyword][*]consulta=query
[keyword][]fim=end
[condition][woolfel.ecommerce.model.Customer]the Customer=cust : Customer()
[condition][woolfel.ecommerce.model.Customer]- has an email=emailAddress != null
[condition][woolfel.ecommerce.model.Customer]- first name is "{first}"=first == "{first}"
[condition][woolfel.ecommerce.model.Customer]- last name is "{surname}"=sname: surname == "{surname}"
[condition][woolfel.ecommerce.model.CustomerProfile]a User profile=userprof : CustomerProfile(prfid : profileId)
[condition][woolfel.ecommerce.model.Response]the Response=resp : Response()
[condition][woolfel.ecommerce.model.Response]- is empty=userId == usrid, recommendation == null
[condition][woolfel.ecommerce.model.Response]- is not empty=userId == usrid, recommendation != null
[condition][woolfel.ecommerce.model.Response]- matches the user=userId == userid
[condition][woolfel.ecommerce.model.Aggregate]an aggregate=aggr : Aggregate()
[condition][woolfel.ecommerce.model.Recommendation]the recommendation where=recm : Recommendation()
[condition][woolfel.ecommerce.model.Recommendation]- the profile is found=profileId == prfid
[condition][woolfel.ecommerce.model.Product]the Product=prod : Product()
[condition][woolfel.ecommerce.model.Product]- store is "{store}"=storeCategory == "{store}"
[condition][woolfel.ecommerce.model.Product]- shop category is "{shopcat}"=shopCategory == "{shopcat}"
[condition][woolfel.ecommerce.model.Product]- category is "{prodcat}"=productCategory == "{prodcat}"
[condition][woolfel.ecommerce.model.Product]- subcategory is "{subcat}"=subProductCategory == "{subcat}"
[condition][woolfel.ecommerce.model.Product]- manufacturer is "{manufac}"=manufacturer == "{manufac}"
[condition][woolfel.ecommerce.model.Product]- SKU is equal to "{sku}"=SKU == "{sku}"
[condition][woolfel.ecommerce.model.Order]the Order where=ordr : Order()
[condition][woolfel.ecommerce.model.Order]- shipping method is "{shipmethod}"=shippingMethod == "{shipmethod}"
[condition][woolfel.ecommerce.model.Order]- has more than {items}=cartItems > {items}
[condition][woolfel.ecommerce.model.Order]- has coupons=coupons != null
[consequence][woolfel.ecommerce.model.Recommendation]return the recommendation=resp.setRecommendation(recm);
[consequence][]Log "{msg}"=System.out.println("{msg}");
[condition][]but not=not
[condition][woolfel.ecommerce.model.Customer]- last name is not "{surname}"=surname != "{surname}"
[consequence][*]Show last name=System.out.println(cust.getSurname());
